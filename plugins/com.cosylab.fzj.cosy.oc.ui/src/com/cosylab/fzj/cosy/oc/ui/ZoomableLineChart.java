/*
 * Copyright (c) 2017 Cosylab d.d.
 *
 * Contact Information:
 *   Cosylab d.d., Ljubljana, Slovenia
 *   http://www.cosylab.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Eclipse Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * For more information about the license please refer to the LICENSE
 * file included in the distribution.
 */
package com.cosylab.fzj.cosy.oc.ui;

import com.cosylab.fzj.cosy.oc.ui.util.HorizontalAxis;
import com.cosylab.fzj.cosy.oc.ui.util.SymmetricAxis;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * <code>ZoomableLineChart</code> is a decoration for the {@link LineChart}, which provides zooming capabilities. When
 * user drags the mouse cursor across the surface of the chart (from left to right) a green rectangle is drawn to mark
 * the zoomed in area. On mouse release action the axes ranges are adjusted to match the zoom rectangle. The zoom
 * rectangle should be at least 5 pixels high and 5 pixels wide. Zoom out happens on double click. This class does not
 * provide generic zoom - it is intended to be used only with the orbit correction.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class ZoomableLineChart extends StackPane {

    private static final int MIN_ZOOM_THRESHOLD = 5;
    private final LineChart<Number,Number> chart;
    private final HorizontalAxis xAxis;
    private final SymmetricAxis yAxis;
    private final Rectangle zoomRect;
    private final boolean useXAutoRangeForDefaultZoom;
    private final double defaultXMin;
    private final double defaultXMax;
    private double defaultYMin = Double.NaN;
    private double defaultYMax = Double.NaN;
    private final double defaultTickX;
    private double defaultTickY = Double.NaN;
    private final boolean horizontalZoom;
    private final boolean verticalZoom;
    private BooleanProperty defaultZoomProperty = new SimpleBooleanProperty(true);
    private BooleanProperty inhibitZoomProperty = new SimpleBooleanProperty(false);

    /**
     * Constructs a new zoomable chart without any labels.
     */
    public ZoomableLineChart(LineChart<Number,Number> chart, boolean useXAutoRangeForDefaultZoom,
            boolean horizontalZoom, boolean verticalZoom) {
        this.chart = chart;
        this.useXAutoRangeForDefaultZoom = useXAutoRangeForDefaultZoom;
        this.xAxis = (HorizontalAxis)chart.getXAxis();
        this.yAxis = (SymmetricAxis)chart.getYAxis();
        this.defaultXMin = xAxis.getLowerBound();
        this.defaultXMax = xAxis.getUpperBound();
        this.defaultTickX = xAxis.getTickUnit();
        this.defaultTickY = yAxis.getTickUnit();
        this.horizontalZoom = horizontalZoom;
        this.verticalZoom = verticalZoom;
        this.zoomRect = new Rectangle();
        this.zoomRect.setManaged(false);
        this.zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0,1,1,0.5));
        this.zoomRect.setStroke(Color.DARKSEAGREEN.darker());
        getChildren().addAll(this.chart,this.zoomRect);
        setUpZooming();
    }

    /**
     * Returns the property which specifies if the zooming is allowed or inhibited.
     *
     * @return the property defining if zoom is allowed or not
     */
    public BooleanProperty inhibitZoomProperty() {
        return inhibitZoomProperty;
    }

    private double[] snap(double x, double y) {
        Bounds bounds = chart.getLayoutBounds();
        Bounds xAxisBounds = xAxis.getLayoutBounds();
        Bounds yAxisBounds = yAxis.getLayoutBounds();
        Insets padding = chart.getPadding();
        if (x > bounds.getMaxX() - padding.getRight() - 1) {
            x = bounds.getMaxX() - padding.getRight() - 1;
        }
        if (xAxis.isTickLabelsVisible() && !verticalZoom) {
            if (y > yAxisBounds.getMaxY() - 2) {
                y = yAxisBounds.getMaxY() - 2;
            }
        } else {
            if (y > bounds.getMaxY() - xAxisBounds.getMaxY() - 1) {
                y = bounds.getMaxY() - xAxisBounds.getMaxY() - 1;
            }
        }
        if (x < yAxisBounds.getMaxX() + yAxis.getTickLabelGap() + padding.getLeft()){
            x = yAxisBounds.getMaxX() + yAxis.getTickLabelGap() + padding.getLeft();
        }
        if (y < padding.getTop() + 1) {
            y = padding.getTop() + 1;
        }
        return new double[]{x,y};
    }

    private void setUpZooming() {
        final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
        mouseAnchor.set(new Point2D(0,0));
        chart.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1) {
                defaultZoom();
            }
        });
        chart.setOnMousePressed(e -> {
            if (inhibitZoomProperty.get()) return;
            double[] snap = snap(e.getX(),e.getY());
            mouseAnchor.set(new Point2D(snap[0],snap[1]));
            zoomRect.setWidth(0);
            zoomRect.setHeight(0);
        });
        chart.setOnMouseDragged(e -> {
            if (inhibitZoomProperty.get()) return;
            double x = e.getX();
            double y = e.getY();
            double xAnchor = mouseAnchor.get().getX();
            double[] snap = snap(e.getX(),e.getY());
            x = snap[0];
            y = snap[1];
            if (horizontalZoom) {
                zoomRect.setX(Math.min(x,xAnchor));
                zoomRect.setWidth(Math.abs(x - xAnchor));
            } else {
                zoomRect.setX(snap(0,0)[0]);
                zoomRect.setWidth(snap(chart.getWidth(),chart.getHeight())[0]);
            }
            if (verticalZoom) {
                zoomRect.setY(Math.min(y,mouseAnchor.get().getY()));
                zoomRect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
            } else {
                zoomRect.setY(snap(0,0)[1]);
                zoomRect.setHeight(snap(chart.getWidth(),chart.getHeight())[1]);
            }
        });
        chart.setOnMouseReleased(e -> {
            if (zoomRect.getWidth() < MIN_ZOOM_THRESHOLD || zoomRect.getHeight() < MIN_ZOOM_THRESHOLD) {
                zoomRect.setWidth(0);
                zoomRect.setHeight(0);
            } else {
                doZoom();
            }
        });
        defaultZoom();
    }

    public void defaultZoom() {
        if (horizontalZoom) {
            xAxis.setTickUnit(defaultTickX);
            if (useXAutoRangeForDefaultZoom) {
                xAxis.setAutoRanging(true);
            } else {
                xAxis.setLowerBound(defaultXMin);
                xAxis.setUpperBound(defaultXMax);
            }
        }
        if (verticalZoom) {
            if (!Double.isNaN(defaultYMin) && !Double.isNaN(defaultYMax)) {
                yAxis.setLowerBound(defaultYMin);
                yAxis.setUpperBound(defaultYMax);
            }
            if (!Double.isNaN(defaultTickY)) {
                yAxis.setTickUnit(defaultTickY);
            }
            yAxis.setAutoRanging(true);
        }
        defaultYMax = Double.NaN;
        defaultYMin = Double.NaN;
        defaultTickY = Double.NaN;
        zoomRect.setWidth(0);
        zoomRect.setHeight(0);
        defaultZoomProperty.set(true);
    }

    /**
     * Zoom the chart in horizontal axis to the given zommed in range.
     *
     * @param low the lower boundary to zoom into
     * @param high the upper boundary to zoom into
     */
    public void doHorizontalZoom(double low, double high) {
        if (defaultZoomProperty.get()) {
            defaultYMin = yAxis.getLowerBound();
            defaultYMax = yAxis.getUpperBound();
            defaultTickY = yAxis.getTickUnit();
        }
        if (horizontalZoom) {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(low);
            xAxis.setUpperBound(high);
            xAxis.setTickUnit((xAxis.getUpperBound() - xAxis.getLowerBound()) / 6.);
        }
        defaultZoomProperty.set(false);
    }

    private void doZoom() {
        if (defaultZoomProperty.get()) {
            defaultYMin = yAxis.getLowerBound();
            defaultYMax = yAxis.getUpperBound();
            defaultTickY = yAxis.getTickUnit();
        }
        if (horizontalZoom) {
            xAxis.setAutoRanging(false);
            Number low = xAxis.getValueForDisplay(zoomRect.getX() - chart.getPadding().getLeft());
            Number high = xAxis
                    .getValueForDisplay(zoomRect.getX() + zoomRect.getWidth() - chart.getPadding().getLeft());
            xAxis.setLowerBound(low.doubleValue());
            xAxis.setUpperBound(high.doubleValue());
            xAxis.setTickUnit((xAxis.getUpperBound() - xAxis.getLowerBound()) / 6.);
        }
        if (verticalZoom) {
            yAxis.setAutoRanging(false);
            Number high = yAxis.getValueForDisplay(zoomRect.getY() - chart.getPadding().getTop());
            Number low = yAxis.getValueForDisplay(zoomRect.getY() + zoomRect.getHeight() - chart.getPadding().getTop());
            yAxis.setLowerBound(low.doubleValue());
            yAxis.setUpperBound(high.doubleValue());
            yAxis.setTickUnit((yAxis.getUpperBound() - yAxis.getLowerBound()) / 6.);
        }
        zoomRect.setWidth(0);
        zoomRect.setHeight(0);
        defaultZoomProperty.set(false);
    }

    /**
     * Returns the property that defines whether we are currently in default range or zoomed in.
     *
     * @return true if in default range, or false if zoomed in
     */
    public BooleanProperty defaultZoomProperty() {
        return defaultZoomProperty;
    }
}
