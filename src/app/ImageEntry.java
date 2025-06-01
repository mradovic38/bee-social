package app;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class ImageEntry implements Serializable {
    private static final long serialVersionUID = 12234L;

    private final String path;
    private final Image image;
    private final int storerId;

    public ImageEntry(String path, int storerId, Image image) {
        this.path = path;
        this.image = image;
        this.storerId = storerId;
    }

    public String getPath() {
        return path;
    }

    public Image getImage() {
        return image;
    }

    public int getStorerId() {
        return storerId;
    }

    public BufferedImage getBufferedImage() {
        // Convert Image to BufferedImage

        BufferedImage bufferedImage = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return bufferedImage;
    }
}
