package com.twodjnr.render;

import com.twodjnr.core.Component;

public abstract class UIComponent extends Component {
    public UIComponent(String name) {
        super(name);
    }

    @Override
    public abstract void onRender(SpriteBatch batch, Camera camera);
}
