package nafzh.manager;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import javax.swing.JComponent;
public class ComponentPrintable implements Printable {
    private final JComponent componentToPrint;

    public ComponentPrintable(JComponent componentToPrint) {
        this.componentToPrint = componentToPrint;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        double scaleX = pageFormat.getImageableWidth() / componentToPrint.getWidth();
        double scaleY = pageFormat.getImageableHeight() / componentToPrint.getHeight();
        double scale = Math.min(scaleX, scaleY);
        g2d.scale(scale, scale);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        componentToPrint.printAll(g2d);
        return PAGE_EXISTS;
    }
}
