package com.finance_backend.file.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * OCR is optional and should not stop the file upload if it fails.
 * If OCR is not working, the file should still be uploaded successfully.
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final Tesseract tesseract;

    public OcrServiceImpl(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    @Override
    public String extractText(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                log.warn("OCR: uploaded file could not be read as an image, skipping extraction");
                return "";
            }
            return tesseract.doOCR(image);
        } catch (IOException | TesseractException e) {
            log.warn("OCR extraction failed -- continuing without it. Check tessdata path and "
                    + "Microsoft Visual C++ Redistributable if this happens consistently.", e);
            return "";
        }
    }
}
