package com.absinthe.kage.device

class CommandBuilder {

    private val mStringBuilder: StringBuilder = StringBuilder()

    fun with(command: Command): CommandBuilder {
        mStringBuilder.append(command.cmd)
        return this
    }

    fun append(param: String?): CommandBuilder {
        mStringBuilder.append(Command.DELIMITER).append(param)
        return this
    }

    fun build(): String {
        return mStringBuilder.toString()
    }

}