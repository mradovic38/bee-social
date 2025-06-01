package app;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public class ImageEntry implements Serializable {
    private static final long serialVersionUID = 12234L;

    private final String path;
    private final int storerId;
    private transient Image image; // mora transient slike nisu serializable, ali bajtovi jesu
    private byte[] imageBytes;

    public ImageEntry(String path, int storerId, Image image) {
        this.path = path;
        this.storerId = storerId;
        this.image = image;
        this.imageBytes = imageToBytes(image); // serialize-ready
    }

    public String getPath() {
        return path;
    }

    public int getStorerId() {
        return storerId;
    }

    public Image getImage() {
        if (image == null && imageBytes != null) {
            image = bytesToImage(imageBytes);
        }
        return image;
    }

    public BufferedImage getBufferedImage() {
        Image img = getImage();
        BufferedImage bufferedImage = new BufferedImage(
                img.getWidth(null),
                img.getHeight(null),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return bufferedImage;
    }

    private byte[] imageToBytes(Image image) {
        if (image == null) return null;
        try {
            BufferedImage bufferedImage = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to convert image to bytes", e);
        }
    }

    private Image bytesToImage(byte[] bytes) {
        if (bytes == null) return null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            return ImageIO.read(bais);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to convert bytes to image", e);
        }
    }

    // Optional: ensure rehydration during deserialization
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.image = bytesToImage(this.imageBytes);
    }
}
