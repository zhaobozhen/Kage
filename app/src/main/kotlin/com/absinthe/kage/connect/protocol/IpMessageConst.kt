package com.absinthe.kage.connect.protocol

object IpMessageConst {

    const val VERSION = 0x001 // Version
    const val IP_MSG_BR_ENTRY = 0x00000001 //Online
    const val IP_MSG_BR_EXIT = 0x00000002 //Offline
    const val IP_MSG_ANS_ENTRY = 0x00000003 //Online response
    const val IP_MSG_BR_ABSENCE = 0x00000004
    const val IS_ONLINE = 0x00000001 //Heartbeat
    const val GET_DEVICE_INFO = 0x00000002 //Get device info
    const val PROMPT_PHONE_CONNECT = 0x00000003 //Device connected response
    const val MEDIA_SET_PLAYER_STATUS = 0x00000004 //Set player status
    const val MEDIA_SET_PLAYING_STATE = 0x00000005 //Set playing state
    const val MEDIA_PLAY_PREVIOUS = 0x00000006 //Play previous media
    const val MEDIA_PLAY_NEXT = 0x00000007 //Play next media
    const val MEDIA_STOP = 0x00000008 //Stop media
    const val MEDIA_PREPARE_PLAY = 0x00000009 //Prepare play media
    const val MEDIA_IMAGE_INFO = 0x0000000A //Get image info like URI etc.
    const val MEDIA_GET_PLAYING_STATE = 0x0000000B //Get player status
    const val MEDIA_GET_PLAYER_STATUS = 0x0000000C //Get playing state
    const val DEVICE_ROTATION = 0x0000000D //Device rotate
    const val MEDIA_AUDIO_INFO = 0x0000000E //Get audio info like URI etc.
    const val MEDIA_PAUSE = 0x0000000F //Pause playing media
    const val MEDIA_SEEK_TO = 0x00000010 //Media seek to position
    const val MEDIA_GET_DURATION = 0x00000011 //Get media duration
    const val RESPONSE_SET_PLAYBACK_PROGRESS = 0x00000012 //Set media playback progress
    const val RESPONSE_SET_MEDIA_DURATION = 0x00000013 //Set media duration
    const val RESPONSE_PLAYING_INDEX = 0x00000014
    const val RESPONSE_SET_AUDIO_MODE = 0x00000015
    const val MEDIA_GET_PLAYING_POSITION = 0x00000016
    const val MEDIA_PLAY_AUDIO_LIST = 0x00000017
    const val MEDIA_SET_AUDIO_MODE = 0x00000018
    const val MEDIA_SET_PLAY_INDEX = 0x00000019
    const val MEDIA_RESUME_PLAY = 0x0000001A
    const val MEDIA_VIDEO_INFO = 0x0000001B //Get video info like URI etc.

}