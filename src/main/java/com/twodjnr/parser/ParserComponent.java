package com.twodjnr.parser;

import com.twodjnr.core.Component;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class ParserComponent extends Component {
    public ParserComponent() {}

    public ParserComponent(String name) {
        super(name);
    }

    public abstract Component parse(JsonNode node, Parser parser);
}
