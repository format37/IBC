#Requires AutoHotkey v2.0

WinWait "BlueStacks App Player"
WinActivate  ;

MouseClick "Left", 600, 200 ;

WinWait "Second Factor Authentication"
WinActivate  ;

Send "^v" ;

Send "{Enter}" ;