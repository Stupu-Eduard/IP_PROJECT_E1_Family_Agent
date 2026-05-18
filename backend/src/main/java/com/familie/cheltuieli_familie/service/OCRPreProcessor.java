package com.familie.cheltuieli_familie.service;

import nu.pattern.OpenCV;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class OCRPreProcessor {

    private static volatile boolean openCvLoaded = false;
    private static final Logger logger = LoggerFactory.getLogger(OCRPreProcessor.class);

    private static synchronized void ensureOpenCvLoaded() {
        if (!openCvLoaded) {
            try {
                System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
                logger.info("Loaded system OpenCV library: {}", org.opencv.core.Core.NATIVE_LIBRARY_NAME);
            } catch (UnsatisfiedLinkError e) {
                logger.warn("Failed to load system OpenCV, attempting local load as fallback: {}", e.getMessage());
                OpenCV.loadLocally();
            }
            openCvLoaded = true;
        }
    }

    public OCRPreProcessor() {
        ensureOpenCvLoaded();
    }

    private Mat toGrayScale(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    private BufferedImage matToBufferedImage(Mat matrix) throws IOException {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", matrix, mob);
        return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
    }

    private Mat upscale(Mat src, double factor) {
        Mat upscaled = new Mat();
        Imgproc.resize(
                src,
                upscaled,
                new org.opencv.core.Size(0, 0),
                factor,
                factor,
                Imgproc.INTER_CUBIC
        );
        return upscaled;
    }

    private Mat cropRevolut(Mat src) {
        int topCut = (int) (src.rows() * 0.15);
        int bottomCut = (int) (src.rows() * 0.13);

        org.opencv.core.Rect roi = new org.opencv.core.Rect(
                0,
                topCut,
                src.cols(),
                src.rows() - topCut - bottomCut
        );

        return new Mat(src, roi);
    }

    private Mat cropBT(Mat src) {
        int topCut = (int) (src.rows() * 0.115);
        int bottomCut = (int) (src.rows() * 0.08);

        org.opencv.core.Rect roi = new org.opencv.core.Rect(
                0,
                topCut,
                src.cols(),
                src.rows() - topCut - bottomCut
        );

        return new Mat(src, roi);
    }

    private Mat cropING(Mat src) {
        int topCut = (int) (src.rows() * 0.18);
        int bottomCut = (int) (src.rows() * 0.20);

        org.opencv.core.Rect roi = new org.opencv.core.Rect(
                0,
                topCut,
                src.cols(),
                src.rows() - topCut - bottomCut
        );

        return new Mat(src, roi);
    }

    public BufferedImage processImage(File file, String bank) throws IOException {

        ensureOpenCvLoaded();

        if (file == null || !file.exists()) {
            throw new IllegalArgumentException(
                    "Couldn't read the image from the file: " +
                            (file == null ? "null" : file.getName())
            );
        }

        Mat src = Imgcodecs.imread(file.getAbsolutePath());

        if (src.empty()) {
            throw new IllegalArgumentException(
                    "Couldn't read the image from the file: " +
                            file.getName()
            );
        }

        Mat upscaled = upscale(src, 3.0);

        Mat bankCropped;
        String bankKey = bank != null ? bank.toLowerCase() : "";

        switch (bankKey) {
            case "revolut":
                bankCropped = cropRevolut(upscaled);
                break;

            case "bt":
                bankCropped = cropBT(upscaled);
                break;

            case "ing":
                bankCropped = cropING(upscaled);
                break;

            default:
                bankCropped = upscaled.clone();
                break;
        }

        Mat gray = toGrayScale(bankCropped);
        // Removed aggressive equalizeHist and Otsu thresholding as they can wash out thermal print
        
        BufferedImage result = matToBufferedImage(gray);

        src.release();
        upscaled.release();
        bankCropped.release();
        gray.release();

        return result;
    }

    public List<BufferedImage> processPdf(File pdfFile, String bank) throws IOException {

        ensureOpenCvLoaded();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            List<BufferedImage> results = new ArrayList<>();

            Path baseTempDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "secure-temp");
            Files.createDirectories(baseTempDir);
            Path secureTempDir = Files.createTempDirectory(baseTempDir, "pdf_preprocess_");

            try {
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);

                    Path tempPath = Files.createTempFile(secureTempDir, "page_" + page, ".png");
                    File tempFile = tempPath.toFile();

                    try {
                        ImageIO.write(image, "png", tempFile);
                        BufferedImage processed = processImage(tempFile, bank);
                        results.add(processed);
                        logger.info("Preprocessed page: {}", (page + 1));
                    } finally {
                        Files.deleteIfExists(tempPath);
                    }
                }
            } finally {
                org.springframework.util.FileSystemUtils.deleteRecursively(secureTempDir);
            }

            return results;
        }
    }
}