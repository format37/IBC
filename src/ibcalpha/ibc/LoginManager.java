// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2018 Richard L King (rlking@aultan.com)
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBC is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBC.  If not, see <http://www.gnu.org/licenses/>.

package ibcalpha.ibc;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class LoginManager {

    private static LoginManager _LoginManager;

    static {
        _LoginManager = new DefaultLoginManager();
    }

    public static void initialise(LoginManager loginManager){
        if (loginManager == null) throw new IllegalArgumentException("loginManager");
        _LoginManager = loginManager;
    }

    public static void setDefault() {
        _LoginManager = new DefaultLoginManager();
    }

    public static LoginManager loginManager() {
        return _LoginManager;
    }

    public enum LoginState{
        LOGGED_OUT,
        LOGGED_IN,
        LOGGING_IN,
        TWO_FA_IN_PROGRESS,
        LOGIN_FAILED,
        AWAITING_CREDENTIALS
    }

    boolean readonlyLoginRequired() {
        boolean readOnly = Settings.settings().getBoolean("ReadOnlyLogin", false);
        if (readOnly && SessionManager.isGateway()) {
            Utils.logError("Read-only login not supported by Gateway");
            return false;
        }
        return readOnly;
    }
    
    private volatile JFrame loginFrame = null;
    JFrame getLoginFrame() {
        return loginFrame;
    }

    void setLoginFrame(JFrame window) {
        loginFrame = window;
    }
    
    private volatile LoginState loginState = LoginState.LOGGED_OUT;
    public LoginState getLoginState() {
        return loginState;
    }

    public void setLoginState(LoginState state) {
        if (state == loginState) return;
        loginState = state;
        if (null != loginState) switch (loginState) {
            case TWO_FA_IN_PROGRESS:
                Utils.logToConsole("Second Factor Authentication initiated");
                Utils.logToConsole("Calling AHK");
                
                // Get TOTP secret from settings
                String totpSecret = Settings.settings().getString("totp_secret", "");
                if (!totpSecret.isEmpty()) {
                    // Generate current TOTP code
                    String totpCode = generateTOTP(totpSecret);
                    Utils.logToConsole("Current TOTP Code: " + totpCode);
                }
                
                // // Execute ibkr-2fa.exe
                // try {
                //     // String ahkScriptPath = LoginManager.loginManager().getAhkPathFromSettings();
                //     String ahkScriptPath = Settings.settings().getString("ahk_path", "ibkr-2fa.ahk");
                //     ProcessBuilder processBuilder = new ProcessBuilder("C:\\Progra~1\\AutoHotkey\\v2\\AutoHotkey64.exe", ahkScriptPath);
                //     Process process = processBuilder.start();
                //     Utils.logToConsole("ibkr-2fa.ahk started");
                    
                //     // Optionally, if you need to wait for the process to complete:
                //     // int exitCode = process.waitFor();
                //     // Utils.logToConsole("ibkr-2fa.exe completed with exit code: " + exitCode);
                // } catch (Exception e) {
                //     Utils.logToConsole("Error executing ibkr-2fa.exe: " + e.getMessage());
                // }
                if (LoginStartTime == null) LoginStartTime = Instant.now();
                break;
            case LOGGING_IN:
                if (LoginStartTime == null) LoginStartTime = Instant.now();
                break;
            case LOGGED_IN:
                Utils.logToConsole("Login has completed");
                if (shutdownAfterTimeTask != null) {
                    shutdownAfterTimeTask.cancel(false);
                    shutdownAfterTimeTask = null;
                }   break;
            default:
                break;
        }
    }

    private Instant LoginStartTime;
    private ScheduledFuture<?> shutdownAfterTimeTask;

    void secondFactorAuthenticationDialogClosed() {
        if (LoginStartTime == null) {
            // login did not proceed from the SecondFactorAuthentication dialog - for
            // example because no second factor device could be selected
            return;
        }
        
        // Second factor authentication dialog timeout period
        final int SecondFactorAuthenticationTimeout = Settings.settings().getInt("SecondFactorAuthenticationTimeout", 180);

        // time (seconds) to allow for login to complete before exiting
        final int exitInterval = Settings.settings().getInt("SecondFactorAuthenticationExitInterval", 60);

        final Duration d = Duration.between(LoginStartTime, Instant.now());
        LoginStartTime = null;
        
        Utils.logToConsole("Duration since login: " + d.getSeconds() + " seconds");

        if (d.getSeconds() < SecondFactorAuthenticationTimeout) {
            // The 2FA prompt must have been handled by the user, so authentication
            // should be under way
            
            if (SessionManager.isFIX()) {
                // no Splash screen is dislayed for FIX Gateway - just let things run
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGED_IN);
                return;
            }
            
            if (!reloginPermitted()) {
                // just let loading continue
                return;
            }

            Utils.logToConsole("If login has not completed, IBC will exit in " + exitInterval + " seconds");
            restartAfterTime(exitInterval, "IBC closing because login has not completed after Second Factor Authentication");
            return;
        }
        
        if (!reloginPermitted()) {
            Utils.logToConsole("Re-login after second factor authentication timeout not required");
            return;
        }
        
        // The 2FA prompt hasn't been handled by the user, so we re-initiate the login
        // sequence after a short delay
        Utils.logToConsole("Re-login after second factor authentication timeout in 5 second");
        MyScheduledExecutorService.getInstance().schedule(() -> {
            GuiDeferredExecutor.instance().execute(
                () -> {getLoginHandler().initiateLogin(getLoginFrame());}
            );
        }, 5, TimeUnit.SECONDS);
    }
    
    private boolean reloginPermitted() {
        if (Settings.settings().getString("ReloginAfterSecondFactorAuthenticationTimeout", "").isEmpty()) {
            if (!Settings.settings().getString("ExitAfterSecondFactorAuthenticationTimeout", "").isEmpty()) {
                return Settings.settings().getBoolean("ExitAfterSecondFactorAuthenticationTimeout", false);
            }
            return false;
        }
        return Settings.settings().getBoolean("ReloginAfterSecondFactorAuthenticationTimeout", false);
    }
    
    void restartAfterTime(final int secondsTillShutdown, final String message) {
        try {
            shutdownAfterTimeTask = MyScheduledExecutorService.getInstance().schedule(()->{
                GuiExecutor.instance().execute(()->{
                    if (getLoginState() == LoginManager.LoginState.LOGGED_IN) {
                        Utils.logToConsole("Login has already completed - no need for IBC to exit");
                        return;
                    }
                    Utils.exitWithError(ErrorCodes.SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT, message);
                });
            }, secondsTillShutdown, TimeUnit.SECONDS);
        } catch (Throwable e) {
            Utils.exitWithException(99999, e);
        }
    }

    // TOTP generation methods
    private String generateTOTP(String base32Secret) {
        try {
            // Decode the Base32 secret
            byte[] key = base32Decode(base32Secret);
            
            // Get current time and calculate counter value (30 second interval)
            long counter = System.currentTimeMillis() / 1000 / 30;
            
            // Generate OTP using counter
            return generateOTP(key, counter, 6);
        } catch (Exception e) {
            Utils.logToConsole("Error generating TOTP: " + e.getMessage());
            return "Error";
        }
    }

    private String generateOTP(byte[] key, long counter, int digits) throws NoSuchAlgorithmException, InvalidKeyException {
        // Convert counter to byte array
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte)(counter & 0xff);
            counter >>= 8;
        }
        
        // Generate HMAC-SHA1
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec spec = new SecretKeySpec(key, "HmacSHA1");
        mac.init(spec);
        byte[] hash = mac.doFinal(counterBytes);
        
        // Dynamic truncation
        int offset = hash[hash.length - 1] & 0xf;
        int binary = 
            ((hash[offset] & 0x7f) << 24) |
            ((hash[offset + 1] & 0xff) << 16) |
            ((hash[offset + 2] & 0xff) << 8) |
            (hash[offset + 3] & 0xff);
        
        // Calculate modulus
        int otp = binary % (int)Math.pow(10, digits);
        
        // Convert to string with leading zeros if needed
        String result = Integer.toString(otp);
        while (result.length() < digits) {
            result = "0" + result;
        }
        
        return result;
    }

    private byte[] base32Decode(String base32) {
        // Remove padding if present
        base32 = base32.replaceAll("=", "");
        
        // Convert to uppercase for consistency
        base32 = base32.toUpperCase();
        
        // Base32 character set
        String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        
        // Prepare output
        int numBytes = base32.length() * 5 / 8;
        byte[] output = new byte[numBytes];
        
        // Process in chunks of 8 characters (40 bits)
        int bitsLeft = 0;
        int currentByte = 0;
        int outputIndex = 0;
        
        for (int i = 0; i < base32.length(); i++) {
            char c = base32.charAt(i);
            int value = BASE32_CHARS.indexOf(c);
            
            if (value < 0) continue; // Skip non-base32 chars
            
            // Add 5 bits to buffer
            currentByte = (currentByte << 5) | value;
            bitsLeft += 5;
            
            // If we have at least 8 bits, write a byte
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                output[outputIndex++] = (byte)((currentByte >> bitsLeft) & 0xFF);
            }
        }
        
        return output;
    }

    public abstract void logDiagnosticMessage();

    public abstract String FIXPassword();

    public abstract String FIXUserName();

    public abstract String IBAPIPassword();

    public abstract String IBAPIUserName();

    public abstract AbstractLoginHandler getLoginHandler();

    public abstract void setLoginHandler(AbstractLoginHandler handler);

}