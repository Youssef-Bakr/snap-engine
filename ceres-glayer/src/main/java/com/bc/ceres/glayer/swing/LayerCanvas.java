package com.bc.ceres.glayer.swing;

import com.bc.ceres.glayer.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A preliminary UI class.
 * <p>A Swing component capable of drawing a collection of {@link com.bc.ceres.glayer.GraphicalLayer}s.
 * </p>
 *
 * @author Norman Fomferra
 */
public class LayerCanvas extends JComponent implements ViewportAware {

    private CollectionLayer collectionLayer;
    private Viewport viewport;
    private Rectangle2D modelArea;

    private SliderPopUp sliderPopUp;

    public LayerCanvas() {
        this(new CollectionLayer());
    }

    public LayerCanvas(CollectionLayer collectionLayer) {
        this(collectionLayer, new DefaultViewport());
    }

    public LayerCanvas(final CollectionLayer collectionLayer, final Viewport viewport) {
        setOpaque(false);

        this.collectionLayer = collectionLayer;
        this.viewport = viewport;
        this.modelArea = collectionLayer.getBoundingBox(); // todo - register PCL for "collectionLayer.boundingBox"
        this.sliderPopUp = new SliderPopUp();

        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);

        collectionLayer.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                repaint(); // todo - be more specific
            }
        });

        viewport.addChangeListener(new Viewport.ChangeListener() {
            public void handleViewportChanged(Viewport viewport) {
                repaint();
            }
        });

        final NavControl navControl = new NavControl();
        navControl.setBounds(0, 0, 120, 120);
        add(navControl);
        navControl.addSelectionListener(new NavControl.SelectionListener() {
            public void handleRotate(double rotationAngle) {
                final Rectangle bounds = getBounds();
                final Point2D.Double viewCenter = new Point2D.Double(bounds.x + 0.5 * bounds.width,
                                                                     bounds.y + 0.5 * bounds.height);
                viewport.setModelRotation(Math.toRadians(rotationAngle), viewCenter);
            }

            public void handleMove(double moveDirX, double moveDirY) {
                viewport.move(16 * moveDirX, 16 * moveDirY);
            }
        });
    }

    public CollectionLayer getCollectionLayer() {
        return collectionLayer;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Rectangle2D getModelBounds() {
        return modelArea;
    }

    @Override
    protected void paintChildren(Graphics g) {
        final Graphics2D g2D = (Graphics2D) g;
        final Composite oldComposite = g2D.getComposite();
        try {
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            super.paintChildren(g);
        } finally {
            if (oldComposite != null) {
                g2D.setComposite(oldComposite);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
// dont need to paint background, because I am not opaque and neither is my parent  (make sure!)
//        g.setColor(getBackground());
//        g.fillRect(getX(), getY(), getWidth(), getHeight());

        // ensure clipping is set
        if (g.getClipBounds() == null) {
            g.setClip(getX(), getY(), getWidth(), getHeight());
        }
        final CanvasRendering canvasRendering = new CanvasRendering((Graphics2D) g);
        collectionLayer.render(canvasRendering);
    }

    private double getZoomFactor() {
        return 1.0 / viewport.getModelScale();
    }

    private void setZoomFactor(double zoomFactor) {
        final Rectangle bounds = getBounds();
        final Point2D.Double viewCenter = new Point2D.Double(bounds.x + 0.5 * bounds.width,
                                                             bounds.y + 0.5 * bounds.height);
        setZoomFactor(zoomFactor, viewCenter);
    }

    private void setZoomFactor(double zoomFactor, Point2D viewCenter) {
        viewport.setModelScale(1.0 / zoomFactor, viewCenter);
    }

    private void move(double dx, double dy) {
        viewport.move(dx, dy);
        // todo - compute clip here, move buffered component image, redraw only clipping area
        // System.out.println("translate: deltaView = " + deltaView);
    }

    private class MouseHandler extends MouseInputAdapter {
        private Point p0;
        private Timer timer;

        @Override
        public void mousePressed(MouseEvent e) {
            p0 = e.getPoint();
        }

        @Override
        public void mouseReleased(final MouseEvent mouseEvent) {
            if (mouseEvent.isPopupTrigger()) {
                final Point point = mouseEvent.getPoint();
                SwingUtilities.convertPointToScreen(point, LayerCanvas.this);
                sliderPopUp.show(point);
            } else {
                sliderPopUp.hide();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
//                if (timer != null) {
//
//                    timer.stop();
//                }
//                final Point pv2 = e.getPoint();
//                final double factor1 = getZoomFactor();
//                final double factor2 = 2.0 * getZoomFactor();
//
//                final Point2D pu2 = viewport.getViewToModelTransform().transform(pv2, null);
//
//                final Rectangle bounds = getBounds();
//                final Point2D.Double pv1 = new Point2D.Double(bounds.x + 0.5 * bounds.width,
//                                                                     bounds.y + 0.5 * bounds.height);
//
//                final Point2D pu1 = viewport.getViewToModelTransform().transform(pv1, null);
//
//                timer = new Timer(10, new ActionListener() {
//                    int i = 0;
//                    final int n = 10;
//                    public void actionPerformed(ActionEvent e) {
//                        i++;
//                        if (i == n) {
//                            timer.stop();
//                            return;
//                        }
//                        final double f = i / (n - 1.0);
//                        final double zoomFactorNew = factor1 + f * (factor2 - factor1);
//                        final Point2D.Double pv = new Point2D.Double(
//                               - f * (pv2.getX() - pv1.getX()),
//                                -f * (pv2.getY() - pv1.getY()));
//                        //final Point2D pv = viewport.getModelToViewTransform().deltaTransform(pu, null);
//                        translate(pv);
//                        //setZoomFactor(zoomFactorNew);
//                    }
//                });
//                timer.start();

            }
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            final Point p = e.getPoint();
            final double dx = p.x - p0.x;
            final double dy = p.y - p0.y;
            LayerCanvas.this.move(dx, dy);
            p0 = p;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            final int wheelRotation = e.getWheelRotation();
            final double newZoomFactor = getZoomFactor() * Math.pow(1.1, wheelRotation);
            setZoomFactor(newZoomFactor);
        }

    }

    private class SliderPopUp {
        private JWindow window;
        private JSlider slider;

        public void show(Point location) {
            if (window == null) {
                initUI();
            }
            final double oldZoomFactor = getZoomFactor();
            slider.setValue((int) Math.round(10.0 * Math.log(oldZoomFactor) / Math.log(2.0)));
            window.setLocation(location);
            window.setVisible(true);
        }

        public void hide() {
            if (window != null) {
                window.setVisible(false);
            }
        }

        private void initUI() {
            window = new JWindow();
            final int min = -100;
            final int max = 100;
            slider = new JSlider(min, max);
            slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    final double newZoomFactor = Math.pow(2.0, slider.getValue() / 10.0);
                    setZoomFactor(newZoomFactor);
                    if (!slider.getValueIsAdjusting()) {
                        hide();
                    }
                }
            });

            window.requestFocus();
            window.setAlwaysOnTop(true);
            window.add(slider);
            window.pack();
            window.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    hide();
                }
            });
        }
    }

    private class CanvasRendering implements InteractiveRendering {
        private final Graphics2D graphics2D;

        public CanvasRendering(Graphics2D graphics2D) {
            this.graphics2D = graphics2D;
        }

        public Graphics2D getGraphics() {
            return graphics2D;
        }

        public Viewport getViewport() {
            return viewport;
        }

        public Rectangle2D getBounds() {
            return LayerCanvas.this.getBounds();
        }

        public void invalidateRegion(Rectangle region) {
            repaint(region.x, region.y, region.width, region.height);
        }

        public void invokeLater(Runnable runnable) {
            SwingUtilities.invokeLater(runnable);
        }
    }
}