package nafzh.manager;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class ComponentScreenshot {

    /**
     * كلاس مساعد لتغليف الصورة (BufferedImage) لتكون قابلة للنقل عبر الحافظة المؤقتة (Clipboard).
     */
    private static class ImageTransferable implements Transferable {
        private final BufferedImage image;

        public ImageTransferable(BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }

    /**
     * يلتقط صورة لمكون Swing محدد، ويحفظها في مجلد "سكرين شوت"، وينسخها إلى الحافظة المؤقتة.
     * * @param component المكون المراد التقاط صورته.
     * @param name اسم اللوحة (يستخدم لتسمية الملف).
     * @return true إذا نجح الحفظ والنسخ، false خلاف ذلك.
     */
    public static boolean captureAndSaveToClipboard(JComponent component, String name) {
        if (component == null) {
            System.err.println("Component cannot be null.");
            return false;
        }
        
        // متغير لحفظ حالة النجاح/الفشل
        final boolean[] success = {false};

        try {
            // تنفيذ متزامن لضمان اكتمال عملية الالتقاط قبل العودة
            Runnable captureTask = () -> {
                try {
                    // تحديث المكون لضمان الأبعاد الصحيحة
                    component.revalidate();
                    component.repaint();

                    int w = component.getWidth();
                    int h = component.getHeight();

                    if (w <= 0 || h <= 0) {
                        System.err.println("Component dimensions are zero or negative.");
                        return;
                    }

                    // 1. التقاط الصورة (الرسم على BufferedImage)
                    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = image.createGraphics();
                    component.printAll(g2); // استخدام printAll لرسم المكون بالكامل
                    g2.dispose();

                    // 2. الحفظ في ملف مخصص
                    File outputDir = new File("سكرين شوت"); // مجلد "سكرين شوت"
                    if (!outputDir.exists()) {
                        outputDir.mkdirs(); // إنشاء المجلد إذا لم يكن موجودًا
                    }
                    // تنسيق اسم الملف: Name_Timestamp.png
                    String fileName = String.format("%s_%d.png", name.replace(" ", "_"), System.currentTimeMillis());
                    File targetFile = new File(outputDir, fileName);

                    if (ImageIO.write(image, "png", targetFile)) {
                        System.out.println("Screenshot saved to: " + targetFile.getAbsolutePath());
                        success[0] = true;
                    } else {
                        System.err.println("Could not find a writer for the 'png' format.");
                        success[0] = false;
                        return;
                    }

                    // 3. النسخ إلى الحافظة المؤقتة (Clipboard)
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    ImageTransferable transferable = new ImageTransferable(image);
                    clipboard.setContents(transferable, null);
                    System.out.println("Screenshot copied to clipboard.");

                } catch (IOException e) {
                    System.err.println("Error saving the component image: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("An unexpected error occurred during capture: " + e.getMessage());
                }
            };
            
            // نفذ المهمة على Event Dispatch Thread وانتظر النتيجة
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(captureTask);
            } else {
                captureTask.run();
            }

        } catch (Exception e) {
            System.err.println("Error during synchronous capture/copy: " + e.getMessage());
            return false;
        }

        return success[0];
    }
}