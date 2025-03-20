# Overview
This fork is aimed to automate the 2FA authorization. For this purpose I am using Autohotkey app. As far as I understand, the Autohotkey is available only on windows. If you need to run  this in Linux, you need to find the corresponding alternative and update the following files to work with your app and script:
* src\ibcalpha\ibc\LoginManager.java: ProcessBuilder processBuilder = new ProcessBuilder("C:\\Progra~1\\AutoHotkey\\v2\\AutoHotkey64.exe", ahkScriptPath);
* resources/config.ini: ahk_path=C:\\IBC\\ibkr-2fa.ahk

# Installation
1. Download and install Bluestacks for the Android smartphone emulation with Google Authenticator:
https://www.bluestacks.com/bluestacks-5.html
2. Login to account and configure the Google authenticator to show the IB 2FA code. This window can be minimized, but should not be closed.

3. Download and install Autohotkey for Ctrl+C / Ctrl+V action:
https://www.autohotkey.com/

4. Clone the IBC repo:
git clone https://github.com/format37/IBC.git

5. Copy files from resources directory to c:\IBC\

6. Create Documents\ibc folder, move config.ini to ibc and encrypt ibc folder
Define in the config.ini:
* IbLoginId
* IbPassword
* TradingMode
* ahk_path

7 Update the TWS_MAJOR_VRSN in the following files:
* StartTWS.bat
* StartGateway.bat
For example, if if version is: 10.34.1c then set:
```
TWS_MAJOR_VRSN=1034
```

8. Download and install JDK:
https://www.oracle.com/cis/java/technologies/downloads/#jdk24-windows

9. Download Apache Ant and extract to
https://ant.apache.org/bindownload.cgi

10. Rename or remove old IBC:
```
mv C:\IBC\IBC.jar C:\IBC\IBC_0.jar 
```

11. Compile app
```
java -cp "C:\apache-ant\lib\ant-launcher.jar" org.apache.tools.ant.launch.Launcher -buildfile C:\Users\alex\Downloads\IBC-master\IBC-master\build.xml
```

12. Copy JAR app to execution folder:
```
cp C:\Users\alex\Downloads\IBC-master\IBC-master\resources\IBC.jar C:\IBC
```

13. Now you can start TWS
```
StartTWS.bat
```

# Previous doc revision, with compilation:
Download and install the latest offline TWS: https://www.interactivebrokers.com/en/trading/tws-offline-latest.php

Download IBC: https://github.com/IbcAlpha/IBC/releases

Extract IBC to a new folder: C:\IBC

Create Documents\ibc folder, move config.ini to ibc and encrypt ibc folder

Define

IbLoginId

IbPassword

TradingMode

in the config.ini

Update the TWS_MAJOR_VRSN in the following files:

StartTWS.bat

StartGateway.bat

For example, set

TWS_MAJOR_VRSN=1034

if version is: 10.34.1c

***
Clone the IBC repo
git clone https://github.com/IbcAlpha/IBC.git

Donwload and install JDK
https://www.oracle.com/cis/java/technologies/downloads/#jdk24-windows

Download Apache Ant and extract to
https://ant.apache.org/bindownload.cgi

Modify app
C:\Users\alex\Downloads\IBC-master\IBC-master\src\ibcalpha\ibc\LoginManager.java

Rename old IBC:
mv C:\IBC\IBC.jar C:\IBC\IBC_0.jar 

Compile app
java -cp "C:\apache-ant\lib\ant-launcher.jar" org.apache.tools.ant.launch.Launcher -buildfile C:\Users\alex\Downloads\IBC-master\IBC-master\build.xml

Copy JAR app to execution folder:
cp C:\Users\alex\Downloads\IBC-master\IBC-master\resources\IBC.jar C:\IBC

Compile AHK script with AutoHotkey dash and place ibkr-2fa.exe in the C:\IBC folder

# Original documentation
**Download the
[latest official release here](https://github.com/IbcAlpha/IBC/releases/latest)**

IBC automates many aspects of running [Interactive Brokers](https://www.interactivebrokers.com) [Trader Workstation and Gateway](https://www.interactivebrokers.com/en/index.php?f=14099#tws-software)
that would otherwise involve manual intervention. It's especially useful for
those who run automated trading systems based on the [Interactive Brokers API](http://interactivebrokers.github.io),
but many manual traders find it helpful as well.

Here are some of the things it will do for you:

* It automatically fills in your username and password in the Login
dialog when TWS or Gateway start running, and clicks the Login button
* It can ensure that while a TWS/Gateway session is running, attempts to
logon from another computer or device do not succeed
* It can participate in Two Factor Authentication using IBKR Mobile in such
a way that users who miss the 2FA alert on their device will automatically
have further opportunities without needing to be at the computer
* It handles various dialog boxes which TWS sometimes displays, to keep
things running smoothly with no user involvement
* It allows TWS and Gateway to be auto-restarted each day during the week,
without the user having to re-authenticate
* It allows TWS and Gateway to be shut down at a specified time every day
* It allows TWS to be shut down at a specified time on a specified day
of the week
* It can be remotely instructed to shut down TWS or Gateway, which can
be useful if they are running in the cloud or on an inaccessible computer

IBC runs on Windows, macOS and Linux.

> IMPORTANT NOTICES
>
> Please note that IBC cannot automatically complete your login if
Interactive Brokers have given you a card or device that you must use
during login. IBC can still enter your username and password, but you
will have to type in the relevant code, or use the IBKR Mobile app to
complete the login. You can request Interactive Brokers (via your
Account Management page on their website) to relax this requirement
when logging in to TWS or Gateway, but you will lose certain guarantees
should you suffer losses as a result of your account being compromised.
>
> If you're moving to IBC from IBController, there are some changes
that you'll have to make. See the [IBC User Guide](userguide.md) for
further information.
>
> No guarantee is given that this repository will be in a fully
self-consistent state at all times. In particular, if you build IBC.jar
directly from this repository, you should test thoroughly before
deploying it (an example of this might be when composing a Docker image).


Downloads
---------

If you just want to use IBC without modifying it, you should download
the latest official release ZIP which you can find
[here](https://github.com/IbcAlpha/IBC/releases/latest). Note that
there are separate release files for Windows, macOS and Linux.

Users who want to make changes to IBC should clone this repository
in the usual way.

User Guide
----------

Please see the [IBC User Guide](userguide.md) for installation and
usage instructions. The User Guide is also included as a PDF file in the
download ZIPs.

Support
-------

> IMPORTANT
> By far the most common problem that users have when setting up IBC
is the result of trying to use it with the self-updating version of TWS.
>
>**IBC DOES NOT WORK with the self-updating version of TWS.**
>
>You must install the offline version of TWS for use with IBC.
>
>Note however that there is no self-updating version of the Gateway, so the
normal Gateway installer will work fine if you only want to use the Gateway.

If you need assistance with running IBC, or have any queries or suggestions
for improvement, you should join the [IBC User Group](https://groups.io/g/ibcalpha).

If you're convinced you've found a bug in IBC, please report it via either
the
[IBC User Group](https://groups.io/g/ibcalpha) or the
[GitHub Issue Tracker](https://github.com/IbcAlpha/IBC/issues).
Please provide as much evidence as you can, especially the versions of IBC
and TWS/Gateway you're using and a full description of the incorrect
behaviour you're seeing.

Note that IBC creates a log file that records a lot of useful information
that can be very helpful in diagnosing users' problems. The location of
this log file is prominently displayed in the window that appears when you
run IBC. Please attach this log file to any problem reports.

Contributing
------------

There are several ways you may be able to contribute to IBC's ongoing
development and support. Please read the
[contributor guidelines](CONTRIBUTING.md), and send us a
[pull request](../../pulls).

We also thank past contributors to the IBController project from which
IBC was forked: Richard King, Steven Kearns, Ken Geis, Ben Alex and
Shane Castle.

License
-------

IBC is licensed under the
[GNU General Public License](http://www.gnu.org/licenses/gpl.html) version 3.

History
-------

A brief note by Richard L King (rlktradewright on GitHub) updated
5 April 2019.

IBC is a fork of the original
[IBController project](https://github.com/ib-controller/ib-controller).
For many years, from 2004 to early 2018, I was the primary
maintainer, developer and supporter for that project.

For reasons beyond my control, in early 2018 I decided to withdraw my direct
support for the original project, and to create this fork. It is my intention
to ensure that this fork continues to be developed and supported to the high
standards of the past.

The status of the original IBController repository now seems unclear, so
IBController users are invited to switch to IBC.

If you switch from IBController to IBC, please note that there are some
significant differences, and it's best to install IBC from scratch using
the download on the [Releases page](https://github.com/IbcAlpha/IBC/releases).
The last section of the [IBC User Guide](userguide.md) contains useful
information about these differences.



