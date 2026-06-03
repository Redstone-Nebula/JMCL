import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.*;
import java.util.Iterator;

public class CreateIcon {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: CreateIcon <input_image> <output_ico>");
            System.exit(1);
        }

        BufferedImage source = ImageIO.read(new File(args[0]));
        if (source == null) {
            System.err.println("Cannot read image: " + args[0]);
            System.exit(1);
        }

        int[] sizes = {24, 32, 48, 256};
        int[] bpps = {32, 32, 32, 32};
        byte[][] imageData = new byte[sizes.length][];

        for (int i = 0; i < sizes.length; i++) {
            BufferedImage resized = resize(source, sizes[i], sizes[i]);
            if (sizes[i] <= 48) {
                imageData[i] = encodeBmp(resized);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, "png", baos);
                imageData[i] = baos.toByteArray();
            }
            System.out.println("  Image " + i + ": " + sizes[i] + "x" + sizes[i]
                + ", " + bpps[i] + "bpp, " + imageData[i].length + " bytes");
        }

        createIco(imageData, sizes, args[1]);
        System.out.println("ICO written to: " + args[1]);
    }

    static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(source, 0, 0, width, height, null);
        g2d.dispose();
        return result;
    }

    static byte[] encodeBmp(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int rowSize = ((w * 32 + 31) / 32) * 4;
        int andRowSize = ((w + 31) / 32) * 4;
        int pixelDataSize = rowSize * h;
        int andMaskSize = andRowSize * h;
        int totalSize = 40 + pixelDataSize + andMaskSize;

        byte[] bmp = new byte[totalSize];

        // BITMAPINFOHEADER
        writeIntLE(bmp, 0, 40);
        writeIntLE(bmp, 4, w);
        writeIntLE(bmp, 8, h * 2);
        writeShortLE(bmp, 12, (short) 1);
        writeShortLE(bmp, 14, (short) 32);
        writeIntLE(bmp, 16, 0);
        writeIntLE(bmp, 20, pixelDataSize);
        writeIntLE(bmp, 24, 0);
        writeIntLE(bmp, 28, 0);
        writeIntLE(bmp, 32, 0);
        writeIntLE(bmp, 36, 0);

        // Pixel data (BGRA, bottom-up, no alpha premultiply)
        int[] argb = new int[w * h];
        image.getRGB(0, 0, w, h, argb, 0, w);

        // BMP stores pixels bottom-up (last row first)
        for (int y = 0; y < h; y++) {
            int srcY = h - 1 - y;
            int rowOffset = 40 + srcY * rowSize;
            for (int x = 0; x < w; x++) {
                int pixel = argb[y * w + x];
                int b = (pixel) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int r = (pixel >> 16) & 0xff;
                int a = (pixel >> 24) & 0xff;
                int offset = rowOffset + x * 4;
                bmp[offset] = (byte) b;
                bmp[offset + 1] = (byte) g;
                bmp[offset + 2] = (byte) r;
                bmp[offset + 3] = (byte) a;
            }
        }

        // AND mask (all zeros for fully opaque)
        int andOffset = 40 + pixelDataSize;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < andRowSize * 8 && x < w; x++) {
                int pixel = argb[y * w + x];
                int a = (pixel >> 24) & 0xff;
                if (a < 128) {
                    int byteIdx = andOffset + y * andRowSize + x / 8;
                    bmp[byteIdx] |= (byte) (1 << (7 - (x % 8)));
                }
            }
        }

        return bmp;
    }

    static void createIco(byte[][] imageData, int[] sizes, String outputPath) throws IOException {
        int count = imageData.length;
        int headerSize = 6 + count * 16;
        int dataOffset = headerSize;

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            writeShortLE(fos, 0);
            writeShortLE(fos, 1);
            writeShortLE(fos, count);

            for (int i = 0; i < count; i++) {
                int w = sizes[i] >= 256 ? 0 : sizes[i];
                int h = sizes[i] >= 256 ? 0 : sizes[i];
                fos.write(w);
                fos.write(h);
                fos.write(0);
                fos.write(0);
                writeShortLE(fos, 1);
                writeShortLE(fos, 32);
                writeIntLE(fos, imageData[i].length);
                writeIntLE(fos, dataOffset);
                dataOffset += imageData[i].length;
            }

            for (byte[] data : imageData) {
                fos.write(data);
            }
        }
    }

    static void writeShortLE(OutputStream os, int value) throws IOException {
        os.write(value & 0xff);
        os.write((value >> 8) & 0xff);
    }

    static void writeIntLE(OutputStream os, int value) throws IOException {
        os.write(value & 0xff);
        os.write((value >> 8) & 0xff);
        os.write((value >> 16) & 0xff);
        os.write((value >> 24) & 0xff);
    }

    static void writeIntLE(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        buf[offset + 2] = (byte) ((value >> 16) & 0xff);
        buf[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    static void writeShortLE(byte[] buf, int offset, short value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
    }
}