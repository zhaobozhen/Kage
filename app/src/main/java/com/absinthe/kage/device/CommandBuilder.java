package com.absinthe.kage.device;

import androidx.annotation.NonNull;

public class CommandBuilder {

    private StringBuilder mStringBuilder;

    public CommandBuilder() {
        mStringBuilder = new StringBuilder();
    }

    public CommandBuilder with(Device.Command command) {
        mStringBuilder.append(command.cmd);
        return this;
    }

    public CommandBuilder append(@NonNull String param) {
        mStringBuilder.append(Device.Command.DELIMITER).append(param);
        return this;
    }

    public String build() {
        return mStringBuilder.toString();
    }
}
