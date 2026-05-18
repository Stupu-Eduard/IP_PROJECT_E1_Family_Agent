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

    private Mat enhanceContrast(Mat gray) {
        Mat enhanced = new Mat();
        Imgproc.equalizeHist(gray, enhanced);
        return enhanced;
    }

    private Mat applyOtsuThreshold(Mat gray) {
        Mat thresholded = new Mat();
        Imgproc.threshold(gray, thresholded, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        return thresholded;
    }

    private Mat deskew(Mat src) {
        // Detect skew angle using moments
        Mat gray = src.clone();
        if (gray.channels() > 1) {
            Imgproc.cvtColor(gray, gray, Imgproc.COLOR_BGR2GRAY);
        }

        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        java.util.List<org.opencv.core.MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            gray.release();
            binary.release();
            hierarchy.release();
            return src.clone();
        }

        // Use minAreaRect on the largest contour to estimate skew
        org.opencv.core.RotatedRect maxRect = null;
        double maxArea = 0;
        for (org.opencv.core.MatOfPoint contour : contours) {
            org.opencv.core.RotatedRect rect = Imgproc.minAreaRect(new org.opencv.core.MatOfPoint2f(contour.toArray()));
            double area = rect.size.width * rect.size.height;
            if (area > maxArea) {
                maxArea = area;
                maxRect = rect;
            }
            contour.release();
        }

        gray.release();
        binary.release();
        hierarchy.release();

        if (maxRect == null) {
            return src.clone();
        }

        double angle = maxRect.angle;
        if (maxRect.size.width < maxRect.size.height) {
            angle = 90 + angle;
        }

        if (Math.abs(angle) < 0.5) {
            return src.clone();
        }

        org.opencv.core.Point center = new org.opencv.core.Point(src.cols() / 2.0, src.rows() / 2.0);
        Mat rotMat = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Mat rotated = new Mat();
        Imgproc.warpAffine(src, rotated, rotMat, src.size(), Imgproc.INTER_CUBIC, org.opencv.core.Core.BORDER_CONSTANT, new org.opencv.core.Scalar(255, 255, 255));
        rotMat.release();
        return rotated;
    }

    private Mat denoiseImage(Mat gray) {
        Mat denoised = new Mat();
        Imgproc.medianBlur(gray, denoised, 3);
        return denoised;
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

        Mat upscaled = upscale(src, 2.0);

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

        Mat deskewed = deskew(bankCropped);
        Mat gray = toGrayScale(deskewed);
        Mat denoised = denoiseImage(gray);
        Mat enhanced = enhanceContrast(denoised);
        Mat thresholded = applyOtsuThreshold(enhanced);

        BufferedImage result = matToBufferedImage(thresholded);

        src.release();
        upscaled.release();
        bankCropped.release();
        deskewed.release();
        gray.release();
        denoised.release();
        enhanced.release();
        thresholded.release();

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