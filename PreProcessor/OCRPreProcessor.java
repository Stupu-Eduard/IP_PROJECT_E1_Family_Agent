package com.example.demo;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.photo.Photo;
import org.springframework.stereotype.Service;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.io.ByteArrayInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.util.ArrayList;
import java.util.List;

@Service
public class OCRPreProcessor {
    static{
        OpenCV.loadLocally();
    }
    public BufferedImage processImage(File file, String bank) throws IOException{
        Mat src = Imgcodecs.imread(file.getAbsolutePath());
        if(src.empty()){
            throw new IllegalArgumentException("Couldn't read the image from the file: " + file.getName());
        }
        Mat upscaled  = upscale(src, 2.0);
        Mat bankCropped;
        switch(bank.toLowerCase()){
            case "revolut": bankCropped = cropRevolut(upscaled); break;
            case "bt": bankCropped = cropBT(upscaled); break;
            case "ing": bankCropped = cropING(upscaled); break;
            default: bankCropped = upscaled.clone(); break;
        }
        Mat gray      = toGrayScale(bankCropped);
        Mat sharpened = sharpenImage(gray);
        Mat threshold = applyThresholde(sharpened);
        BufferedImage result = MatToBufferedImage(threshold);
        src.release();
        gray.release();
        sharpened.release();
        upscaled.release();
        threshold.release();
        return result;
    }
    private Mat toGrayScale(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }
    private Mat applyThresholde(Mat gray){
        Mat threshold = new Mat();
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(gray, mean, stddev);
        if(stddev.get(0, 0)[0] < 60){
            Imgproc.threshold(gray, threshold, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        }else{
            Imgproc.adaptiveThreshold(gray, threshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 17, 4);
        }
        return threshold;
    }
    private BufferedImage MatToBufferedImage(Mat matrix) throws IOException{
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", matrix, mob);
        return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
    }
    private Mat upscale(Mat src, double factor){
        Mat upscaled = new Mat();
        Imgproc.resize(src, upscaled, new org.opencv.core.Size(0, 0), factor, factor, Imgproc.INTER_CUBIC);
        return upscaled;
    }
    private Mat sharpenImage(Mat denoised){
        Mat sharpened = new Mat();
        Mat blur = new Mat();
        Imgproc.GaussianBlur(denoised, blur, new org.opencv.core.Size(0, 0), 3);
        Core.addWeighted(denoised, 1.3, blur, -0.3, 0, sharpened);
        blur.release();
        return sharpened;
    }
    public List<BufferedImage> processPdf(File pdfFile, String bank) throws Exception{
        try(PDDocument document = Loader.loadPDF(pdfFile)){
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            List<BufferedImage> results = new ArrayList<>();
            for(int page = 0; page < document.getNumberOfPages(); page++){
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                File tempFile = File.createTempFile("page_" + page, ".png");
                javax.imageio.ImageIO.write(image, "png", tempFile);
                BufferedImage processed = processImage(tempFile, bank);
                tempFile.delete();
                results.add(processed);
                System.out.println("Preprocessed page: " + (page + 1));
            }
            return results;
        }
    }
    private Mat cropRevolut(Mat src) {
        int topCut    = (int)(src.rows() * 0.15);
        int bottomCut = (int)(src.rows() * 0.13);
        org.opencv.core.Rect roi = new org.opencv.core.Rect(
                0, topCut, src.cols(), src.rows() - topCut - bottomCut
        );
        return new Mat(src, roi);
    }
    private Mat cropBT(Mat src) {
        int topCut    = (int)(src.rows() * 0.115);
        int bottomCut = (int)(src.rows() * 0.08);
        org.opencv.core.Rect roi = new org.opencv.core.Rect(
                0, topCut, src.cols(), src.rows() - topCut - bottomCut
        );
        return new Mat(src, roi);
    }
    private Mat cropING(Mat src) {
        int topCut    = (int)(src.rows() * 0.18);
        int bottomCut = (int)(src.rows() * 0.20);
        org.opencv.core.Rect roi = new org.opencv.core.Rect(
                0, topCut, src.cols(), src.rows() - topCut - bottomCut
        );
        return new Mat(src, roi);
    }
}