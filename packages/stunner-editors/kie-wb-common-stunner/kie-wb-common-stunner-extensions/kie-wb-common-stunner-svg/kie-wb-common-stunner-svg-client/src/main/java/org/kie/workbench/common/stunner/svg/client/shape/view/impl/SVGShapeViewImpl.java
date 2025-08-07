/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */


package org.kie.workbench.common.stunner.svg.client.shape.view.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.Picture;
import com.ait.lienzo.client.core.shape.Text;
import com.ait.lienzo.client.core.shape.toolbox.items.ButtonItem;
import com.google.gwt.dom.client.Style;
import org.kie.workbench.common.stunner.client.lienzo.shape.impl.ShapeStateDefaultHandler;
import org.kie.workbench.common.stunner.client.lienzo.shape.view.wires.WiresScalableContainer;
import org.kie.workbench.common.stunner.client.lienzo.shape.view.wires.ext.DecoratedShapeView;
import org.kie.workbench.common.stunner.core.client.resources.StunnerCommonImageResources;
import org.kie.workbench.common.stunner.core.client.shape.ImageDataUriGlyph;
import org.kie.workbench.common.stunner.core.client.shape.ShapeState;
import org.kie.workbench.common.stunner.core.client.shape.view.event.ShapeViewSupportedEvents;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.svg.client.shape.view.SVGBasicShapeView;
import org.kie.workbench.common.stunner.svg.client.shape.view.SVGContainer;
import org.kie.workbench.common.stunner.svg.client.shape.view.SVGPrimitive;
import org.kie.workbench.common.stunner.svg.client.shape.view.SVGPrimitiveShape;
import org.kie.workbench.common.stunner.svg.client.shape.view.SVGShapeView;

public class SVGShapeViewImpl
        extends DecoratedShapeView<SVGShapeViewImpl>
        implements SVGShapeView<SVGShapeViewImpl> {

    private final String name;
    private final SVGPrimitiveShape svgPrimitive;
    private final SVGChildViewHandler childViewHandler;
    private final ShapeStateDefaultHandler shapeStateHandler;
    private List<Text> variables=new ArrayList<>();
    private final Consumer<String> editActionCallback;
    private final Map<String, Group> variableGroups = new HashMap<>();

    @SuppressWarnings("unchecked")
    public SVGShapeViewImpl(final String name,
                            final SVGPrimitiveShape svgPrimitive,
                            final double width,
                            final double height,
                            final boolean resizable) {
        this(name, svgPrimitive, width, height, resizable, null);
    }

    public SVGShapeViewImpl(final String name,
                            final SVGPrimitiveShape svgPrimitive,
                            final double width,
                            final double height,
                            final boolean resizable,
                            final Consumer<String> editActionCallback) {
        super(resizable ? ShapeViewSupportedEvents.ALL_DESKTOP_EVENT_TYPES : ShapeViewSupportedEvents.DESKTOP_NO_RESIZE_EVENT_TYPES,
                new WiresScalableContainer(),
                svgPrimitive.get(),
                width,
                height);
        this.name = name;
        this.svgPrimitive = svgPrimitive;
        this.childViewHandler = new SVGChildViewHandler(this);
        this.shapeStateHandler = createShapeStateDefaultHandler()
                .setBorderShape((() -> this))
                .setBackgroundShape(() -> this);
        this.editActionCallback = editActionCallback;
    }

    protected ShapeStateDefaultHandler createShapeStateDefaultHandler() {
        return new ShapeStateDefaultHandler();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SVGPrimitive getPrimitive() {
        return svgPrimitive;
    }

    @Override
    public SVGShapeViewImpl addChild(final SVGPrimitive<?> child) {
        childViewHandler.addChild(child);
        return this;
    }

    @Override
    public SVGShapeViewImpl addSVGChild(final SVGContainer parent,
                                        final SVGBasicShapeView child) {
        childViewHandler.addSVGChild(parent,
                                     child);
        return this;
    }

    @Override
    public void destroy() {
        svgPrimitive.destroy();
        childViewHandler.clear();
        shapeStateHandler.reset();
        variableGroups.values().forEach(this::removeVariableGroup);
        variableGroups.clear();
        super.destroy();
    }
    private void removeVariableGroup(final Group group) {
        removeChild(group);
    }

    public void addVariableToGroup(final String uuid, final String title) {
        if (variableGroups.containsKey(uuid)) {
            return;
        }

        final Text text = new Text(title, "Verdana", "sans-serif", 8);
        text.setListening(true);
        text.setFillColor("#0073E6");
        text.setTextDecoration("underline");
        text.addNodeMouseClickHandler(event -> {
            if (null != editActionCallback) {
                editActionCallback.accept(uuid);
            }
        });
        text.addNodeMouseEnterHandler(event -> getLayer().getViewport().getElement().getStyle().setCursor(Style.Cursor.POINTER));
        text.addNodeMouseExitHandler(event -> getLayer().getViewport().getElement().getStyle().setCursor(Style.Cursor.DEFAULT));

        final Picture glyph = new Picture(StunnerCommonImageResources.INSTANCE.edit().getSafeUri());

        final Group variableGroup = new Group();
        variableGroup.add(text);
        variableGroup.add(glyph);

        variableGroups.put(uuid, variableGroup);
        drawVariables();
    }


    public void drawVariables() {
        // Clear existing variable groups from the view before re-drawing.
        variableGroups.values().forEach(this::removeVariableGroup);

        if (!variableGroups.isEmpty()) {
            final double glyphWidth = 16;
            final double glyphPadding = 5;
            final double startX = svgPrimitive.getShapeX() + 10.00;
            final double startY = svgPrimitive.getShapeY() + 50.00;
            final double lineSpacing = 15.00;

            int i = 0;
            for (final Group group : variableGroups.values()) {
                final Text text = (Text) group.getChildNodes().get(0);
                final Picture glyph = (Picture) group.getChildNodes().get(1);

                // Center the glyph vertically with the text
                glyph.setX(text.getBoundingBox().getWidth() + glyphPadding).setY(text.getBoundingBox().getHeight() / 2 - glyph.getBoundingBox().getHeight() / 2);

                // Position the group on the shape
                group.setX(startX);
                group.setY(startY + (i * lineSpacing));

                addChild(group);
                i++;
            }
        }
        refresh();
    }


    @Override
    public String getText() {
        return null;
    }
    @Override
    public List<Text> addVariable(Element shape,String UUID,String title){
        addVariableToGroup(UUID, title);
        //variables.add(new Text(title,"Verdana",8));
        return variables;
    }
    @Override
    public List<Text> deleteVariable(String uuid){
        if (variableGroups.containsKey(uuid)) {
            final Group group = variableGroups.get(uuid);
            removeVariableGroup(group);
            variableGroups.remove(uuid);
            drawVariables();
        }
        return variables;
    }
    @Override
    public String getTitle(){
        return null;

    }

    @Override
    public SVGShapeViewImpl setText() {
        // Remove existing Text nodes from view and list
        for (Text textNode : variables) {
            removeChild(textNode);
        }
        if (variables != null && !variables.isEmpty()) {
            double startX = svgPrimitive.getShapeX() + 10.00;
            double startY = svgPrimitive.getShapeY() + 50.00;
            double lineSpacing = 15.00; // adjust as needed for font size

            for (int i = 0; i < variables.size(); i++) {

                Text text = variables.get(i);
                text.setX(startX);
                text.setY(startY + i * lineSpacing);
                text.setListening(true);
                addChild(text);
            }

            refresh();
        }

        return this;
    }
    @Override
    public Collection<SVGPrimitive<?>> getChildren() {
        return childViewHandler.getChildren();
    }

    @Override
    public Collection<SVGBasicShapeView> getSVGChildren() {
        return childViewHandler.getSVGChildren();
    }

    public ShapeStateDefaultHandler getShapeStateHandler() {
        return shapeStateHandler;
    }

    @Override
    public void applyState(final ShapeState shapeState) {
        shapeStateHandler.applyState(shapeState);
    }
}
