package com.cosylab.fzj.cosy.oc.ui;

import static org.csstudio.ui.fx.util.FXUtilities.setGridConstraints;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.csstudio.ui.fx.util.FXUtilities;
import org.csstudio.ui.fx.util.UnfocusableCheckBox;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.cosylab.fzj.cosy.oc.LatticeElementType;
import com.cosylab.fzj.cosy.oc.ui.model.BPM;
import com.cosylab.fzj.cosy.oc.ui.model.Corrector;
import com.cosylab.fzj.cosy.oc.ui.model.LatticeElement;
import com.cosylab.fzj.cosy.oc.ui.util.BorderedTitledPane;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

/**
 * <code>AdvancedDialog</code> can be called from the main view by clicking on the Advanced button. It offers certain
 * advanced features which are only needed in exceptional situation.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class AdvancedDialog extends Dialog {

    private static class CutOff extends GridPane implements ChangeListener<Number> {

        private final Slider slider;
        private final TextField field;
        private final Property<Number> source;

        CutOff(Property<Number> source, boolean percentage) {
            this.source = source;
            setHgap(5);
            getColumnConstraints().setAll(new PercentColumnConstraints(85),new PercentColumnConstraints(15));
            slider = new Slider();
            slider.setMax(percentage ? 100 : 2);
            slider.setMin(0);
            slider.setSnapToTicks(false);
            slider.setMajorTickUnit(percentage ? 10 : 0.2);
            slider.setMinorTickCount(percentage ? 4 : 1);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.valueProperty().bindBidirectional(source);
            slider.getStyleClass().add("slider-pointing");
            field = new TextField();
            field.setEditable(false);
            add(slider,0,0);
            add(field,1,0);
            source.addListener(this);
            setGridConstraints(field,true,true,HPos.LEFT,VPos.TOP,Priority.ALWAYS,Priority.ALWAYS);
            setGridConstraints(slider,true,true,HPos.LEFT,VPos.CENTER,Priority.ALWAYS,Priority.ALWAYS);
            changed(source,null,source.getValue());
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (newValue instanceof Integer) {
                field.setText(String.valueOf(newValue));
            } else {
                field.setText(String.valueOf(((long)(newValue.doubleValue() * 1000)) / 1000.));
            }
        }

        void dispose() {
            slider.valueProperty().unbindBidirectional(source);
            source.removeListener(this);
        }
    }

    private static class Table<T extends LatticeElement> extends TableView<T> {

        private class SelectionTableColumn extends TableColumn<T,Boolean> {

            SelectionTableColumn() {
                super("");
                setCellValueFactory(new PropertyValueFactory<>("enabledWish"));
                setCellFactory(l -> new Cell<>());
                setEditable(true);
                setSortable(false);
                setPrefWidth(30);
                setMaxWidth(30);
                setMinWidth(30);
                selectAllCheckBox = new UnfocusableCheckBox();
                selectAllCheckBox.setSelected(false);
                selectAllCheckBox.setOnAction(e -> getItems()
                        .forEach(te -> te.enabledWishProperty().setValue(selectAllCheckBox.isSelected())));
                setGraphic(selectAllCheckBox);
                MenuItem inverseMI = new MenuItem("Inverse Selection");
                inverseMI.setOnAction(e -> getItems()
                        .forEach(te -> te.enabledWishProperty().setValue(!te.enabledWishProperty().get())));
                final ContextMenu contextMenu = new ContextMenu(inverseMI);
                selectAllCheckBox.setOnMouseReleased(e -> {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        contextMenu.show(selectAllCheckBox,e.getScreenX(),e.getScreenY());
                    }
                });
            }
        }

        private UnfocusableCheckBox selectAllCheckBox;

        Table() {
            setMaxWidth(Double.MAX_VALUE);
            setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            createTable();
            setFixedCellSize(23);
            setEditable(true);
            setSelectionModel(null);
        }

        private void createTable() {
            TableColumn<T,String> nameColumn = new TableColumn<>("Device");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameColumn.setEditable(false);
            TableColumn<T,Boolean> enabledColumn = new SelectionTableColumn();
            TableColumn<T,Double> locationColumn = new TableColumn<>("Location [m]");
            locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
            locationColumn.setEditable(false);
            getColumns().addAll(Arrays.asList(nameColumn,locationColumn,enabledColumn));
        }

        private ChangeListener<Boolean> listener = (a, o, n) -> {
            if (n) {
                boolean all = getItems().stream().allMatch(e -> e.enabledWishProperty().get());
                selectAllCheckBox.setSelected(all);
            } else {
                selectAllCheckBox.setSelected(false);
            }
        };

        void updateTable(List<T> entries) {
            getItems().forEach(e -> e.enabledWishProperty().removeListener(listener));
            getItems().setAll(entries);
            //enabledwishproperty can only have one registered listener
            entries.forEach(e -> e.enabledWishProperty().removeListener(listener));
            entries.forEach(e -> e.enabledWishProperty().addListener(listener));
            selectAllCheckBox.setSelected(getItems().stream().allMatch(e -> e.enabledWishProperty().get()));
        }
    }

    private static class Cell<T extends LatticeElement> extends CheckBoxTableCell<T,Boolean> {

        private static final Background BACKGROUND = new Background(new BackgroundFill(new Color(1,0,0,0.2),null,null));
        private Background defaultBackground = null;

        @SuppressWarnings("unchecked")
        @Override
        public void updateItem(Boolean item, boolean empty) {
            super.updateItem(item,empty);
            TableRow<T> currentRow = getTableRow();
            if (currentRow.getItem() != null && currentRow.getItem().enableDifferentProperty().get()) {
                if (defaultBackground == null) {
                    defaultBackground = currentRow.getBackground();
                }
                currentRow.setBackground(BACKGROUND);
            } else {
                if (defaultBackground != null) {
                    currentRow.setBackground(defaultBackground);
                }
            }
        }
    }

    private final OrbitCorrectionController controller;
    private Table<BPM> horizontalBPMTable;
    private Table<BPM> verticalBPMTable;
    private Table<Corrector> horizontalCorrectorTable;
    private Table<Corrector> verticalCorrectorTable;
    private CutOff horizontalCutOff;
    private CutOff verticalCutOff;
    private CutOff horizontalCorrectionPercentage;
    private CutOff verticalCorrectionPercentage;
    private final Consumer<LatticeElementType> updater = this::update;
    private final Shell parent;

    /**
     * Constructs a new dialog.
     *
     * @param parent the parent shell, used only for positioning of this dialog
     * @param controller the controller which this dialog works with
     */
    public AdvancedDialog(Shell parent, OrbitCorrectionController controller) {
        super((Shell)null);
        this.parent = parent;
        this.controller = controller;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.window.Window#setShellStyle(int)
     */
    @Override
    protected void setShellStyle(int newShellStyle) {
        super.setShellStyle(SWT.CLOSE | SWT.MIN | SWT.MAX | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
        setBlockOnOpen(false);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Advanced Orbit Correction");
    }

    private void update(LatticeElementType type) {
        switch (type) {
            case HORIZONTAL_BPM:
                horizontalBPMTable.updateTable(controller.getHorizontalBPMs());
                break;
            case VERTICAL_BPM:
                verticalBPMTable.updateTable(controller.getVerticalBPMs());
                break;
            case HORIZONTAL_CORRECTOR:
                horizontalCorrectorTable.updateTable(controller.getHorizontalCorrectors());
                break;
            case VERTICAL_CORRECTOR:
                verticalCorrectorTable.updateTable(controller.getVerticalCorrectors());
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent,IDialogConstants.SELECT_ALL_ID,"Apply",true)
                .setToolTipText("Apply BPMs and Correctors settings to the IOC");
        createButton(parent,IDialogConstants.ABORT_ID,"Restore",true)
                .setToolTipText("Restore the BPMs and Correctors settings from the IOC");
        createButton(parent,IDialogConstants.OK_ID,IDialogConstants.CLOSE_LABEL,true);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.SELECT_ALL_ID) {
            controller.writeLatticeStatesToPV();
        } else if (buttonId == IDialogConstants.ABORT_ID) {
            refresh();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void refresh() {
        controller.getHorizontalBPMs().forEach(e -> e.refresh());
        controller.getVerticalBPMs().forEach(e -> e.refresh());
        controller.getHorizontalCorrectors().forEach(e -> e.refresh());
        controller.getVerticalCorrectors().forEach(e -> e.refresh());
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        parent.addDisposeListener(e -> dispose());
        FXUtilities.createFXBridge(composite,this::createScene);
        applyDialogFont(composite);
        controller.addLaticeUpdateCallback(updater);
        update(LatticeElementType.HORIZONTAL_BPM);
        update(LatticeElementType.VERTICAL_BPM);
        update(LatticeElementType.HORIZONTAL_CORRECTOR);
        update(LatticeElementType.VERTICAL_CORRECTOR);
        refresh();
        Platform.runLater(() -> {
            //this gets rid of stupid Java FX bug, when the table header is
            //not accommodating for the vertical scrollbar width
            Point p = getShell().getSize();
            getShell().setSize(p.x + 5,p.y);
        });
        return composite;
    }

    private Scene createScene(Composite parent) {
        Scene scene = new Scene(new BorderPane(createFXContents(parent)));
        scene.getStylesheets().add(OrbitCorrectionView.class.getResource("style.css").toExternalForm());
        return scene;
    }

    private void dispose() {
        controller.removeLaticeUpdateCallback(updater);
        horizontalCutOff.dispose();
        verticalCutOff.dispose();
        horizontalCorrectionPercentage.dispose();
        verticalCorrectionPercentage.dispose();
    }

    private Node createFXContents(Composite parent) {
        GridPane pane = new GridPane();
        Optional<String> background = Optional.of(FXUtilities.toBackgroundColorStyle(parent.getBackground()));
        pane.setStyle(background.get());
        pane.setHgap(20);
        pane.setVgap(10);
        horizontalBPMTable = new Table<>();
        verticalBPMTable = new Table<>();
        horizontalCorrectorTable = new Table<>();
        verticalCorrectorTable = new Table<>();
        Label horizontalBPMLabel = new Label("Horizontal BPMs");
        Label verticalBPMLabel = new Label("Vertical BPMs");
        Label horizontalCorrectorLabel = new Label("Horizontal Correctors");
        Label verticalCorectorLabel = new Label("Vertical Correctors");
        horizontalBPMLabel.setStyle("-fx-font-weight: bold");
        verticalBPMLabel.setStyle("-fx-font-weight: bold");
        horizontalCorrectorLabel.setStyle("-fx-font-weight: bold");
        verticalCorectorLabel.setStyle("-fx-font-weight: bold");
        horizontalCutOff = new CutOff(controller.horizontalCutOffProperty(),false);
        verticalCutOff = new CutOff(controller.verticalCutOffProperty(),false);
        horizontalCorrectionPercentage = new CutOff(controller.horizontalCorrectionFactorProperty(),true);
        verticalCorrectionPercentage = new CutOff(controller.verticalCorrectionFactorProperty(),true);
        BorderedTitledPane horizontalCutOffPane = new BorderedTitledPane("Horizontal SVD Cut Off",horizontalCutOff,
                background);
        BorderedTitledPane verticalCutOffPane = new BorderedTitledPane("Vertical SVD Cut Off",verticalCutOff,
                background);
        BorderedTitledPane horizontalFactorCutOffPane = new BorderedTitledPane("Horizontal Correction Percentage",
                horizontalCorrectionPercentage,background);
        BorderedTitledPane verticalFactorCutOffPane = new BorderedTitledPane("Vertical Correction Percentage",
                verticalCorrectionPercentage,background);
        int height = 60;
        Arrays.asList(verticalCutOffPane,horizontalCutOffPane,verticalFactorCutOffPane,horizontalFactorCutOffPane)
                .forEach(c -> {
                    c.setMinHeight(height);
                    c.setMaxHeight(height);
                });
        pane.add(horizontalBPMLabel,0,0);
        pane.add(horizontalCorrectorLabel,1,0);
        pane.add(verticalBPMLabel,2,0);
        pane.add(verticalCorectorLabel,3,0);
        pane.add(horizontalBPMTable,0,1);
        pane.add(horizontalCorrectorTable,1,1);
        pane.add(verticalBPMTable,2,1);
        pane.add(verticalCorrectorTable,3,1);
        pane.add(horizontalCutOffPane,0,2,2,1);
        pane.add(verticalCutOffPane,2,2,2,1);
        pane.add(horizontalFactorCutOffPane,0,3,2,1);
        pane.add(verticalFactorCutOffPane,2,3,2,1);
        Arrays.asList(horizontalBPMLabel,verticalBPMLabel,horizontalCorrectorLabel,verticalCorectorLabel)
                .forEach(l -> setGridConstraints(l,false,false,HPos.CENTER,VPos.CENTER,Priority.NEVER,Priority.NEVER));
        Arrays.asList(horizontalBPMTable,verticalBPMTable,horizontalCorrectorTable,verticalCorrectorTable)
                .forEach(l -> setGridConstraints(l,true,true,HPos.LEFT,VPos.TOP,Priority.ALWAYS,Priority.ALWAYS));
        Arrays.asList(horizontalCutOffPane,verticalCutOffPane,horizontalFactorCutOffPane,verticalFactorCutOffPane)
                .forEach(l -> setGridConstraints(l,true,true,HPos.LEFT,VPos.TOP,Priority.ALWAYS,Priority.NEVER));
        Arrays.asList(horizontalCutOffPane,verticalCutOffPane,horizontalFactorCutOffPane,verticalFactorCutOffPane)
                .forEach(l -> GridPane.setMargin(l,new Insets(10,0,0,0)));
        return pane;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.window.Window#getConstrainedShellBounds(org.eclipse.swt.graphics.Rectangle)
     */
    @Override
    protected Rectangle getConstrainedShellBounds(Rectangle preferredSize) {
        Rectangle r = super.getConstrainedShellBounds(preferredSize);
        r.width = 1000;
        Rectangle parentR = parent.getBounds();
        int cX = parentR.x + parentR.width / 2;
        int cY = parentR.y + parentR.height / 2;
        r.x = cX - r.width / 2;
        r.y = cY - r.height / 2;
        r.x = r.x < 0 ? 0 : r.x;
        r.y = r.y < 0 ? 0 : r.y;
        return r;
    }
}
