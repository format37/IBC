// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2021 Richard L King (rlking@aultan.com)
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

import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;

public class SecondFactorAuthenticationDialogHandler implements WindowHandler {
    private SecondFactorAuthenticationDialogHandler() {};
    
    static SecondFactorAuthenticationDialogHandler _secondFactorAuthenticationDialogHandler = new SecondFactorAuthenticationDialogHandler();
    
    static SecondFactorAuthenticationDialogHandler getInstance() {
        return _secondFactorAuthenticationDialogHandler;
    }
    
    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void handleWindow(Window window, int eventID) {
        if (eventID == WindowEvent.WINDOW_OPENED) {
            if (LoginManager.loginManager().readonlyLoginRequired()) {
                doReadonlyLogin(window);
            } else if (secondFactorDeviceSelectionRequired(window)) {
                selectSecondFactorDevice(window);
            } else {
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.TWO_FA_IN_PROGRESS);
                
                // Handle TOTP entry automatically after a short delay
                // to allow the UI to properly initialize
                MyScheduledExecutorService.getInstance().schedule(() -> {
                    enterTOTPCode(window);
                }, 1, java.util.concurrent.TimeUnit.SECONDS);
            }
        } else if (eventID == WindowEvent.WINDOW_CLOSED) {
            if (LoginManager.loginManager().readonlyLoginRequired()) {
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGED_IN);
                return;
            }
            LoginManager.loginManager().secondFactorAuthenticationDialogClosed();
        }
    }

    @Override
    public boolean recogniseWindow(Window window) {
        // For TWS this window is a JFrame; for Gateway it is a JDialog
        if (! (window instanceof JDialog || window instanceof JFrame)) return false;
        
        return SwingUtils.titleContains(window, "Second Factor Authentication") ||
            SwingUtils.titleContains(window, "Security Code Card Authentication");
    }

    private void doReadonlyLogin(Window window){
        if (SwingUtils.clickButton(window, "Enter Read Only")) {
            Utils.logToConsole("initiating read-only login.");
        } else {
            Utils.logError("could not initiate read-only login.");
        }
    }
    
    private boolean secondFactorDeviceSelectionRequired(Window window) {
        // this area appears in the Second Factor Authentication dialog when the
        // user has enabled more than one second factor authentication method

        return (SwingUtils.findTextArea(window, "Select second factor device") != null);
    }
    
    private void selectSecondFactorDevice(Window window) {
        JList<?> deviceList = SwingUtils.findList(window, 0);
        if (deviceList == null) {
            Utils.exitWithError(ErrorCodes.CANT_FIND_CONTROL, "could not find second factor device list.");
            return;
        }

        String secondFactorDevice = Settings.settings().getString("SecondFactorDevice", "");
        if (secondFactorDevice.length() == 0) {
            Utils.logError("You should specify the required second factor device using the SecondFactorDevice setting in config.ini");
            return;
        }

        ListModel<?> model = deviceList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            String entry = model.getElementAt(i).toString().trim();
            if (entry.equals(secondFactorDevice)) {
                deviceList.setSelectedIndex(i);

                if (!SwingUtils.clickButton(window, "OK")) {
                    Utils.logError("could not select second factor device: OK button not found");
                }
                return;
            }
        }
        Utils.logError("could not find second factor device '" + secondFactorDevice + "' in the list");
    }
    
    private void enterTOTPCode(Window window) {
        try {
            Utils.logToConsole("Entering TOTP code...");
            
            // Get TOTP secret from settings
            String totpSecret = Settings.settings().getString("totp_secret", "");
            if (totpSecret.isEmpty()) {
                Utils.logError("No TOTP secret configured in settings");
                return;
            }
            
            // Generate TOTP code - we already have the value in logs, but we'll recalculate
            // to ensure we have the current value
            String totpCode = generateTOTP(totpSecret);
            Utils.logToConsole("Entering TOTP Code: " + totpCode);
            
            // Find input field - usually the first JTextField in the dialog
            JTextField codeField = SwingUtils.findTextField(window, 0);
            if (codeField == null) {
                Utils.logError("Could not find TOTP code input field");
                return;
            }
            
            // Enter the code
            codeField.setText(totpCode);
            
            // Find and click the Continue/Submit button
            if (!SwingUtils.clickButton(window, "Continue") && 
                !SwingUtils.clickButton(window, "Submit") &&
                !SwingUtils.clickButton(window, "OK")) {
                Utils.logError("Could not find Continue/Submit button");
            } else {
                Utils.logToConsole("TOTP code entered and submitted successfully");
            }
        } catch (Exception e) {
            Utils.logError("Error entering TOTP code: " + e.getMessage());
            e.printStackTrace(Utils.getErrStream());
        }
    }
    
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

    private String generateOTP(byte[] key, long counter, int digits) throws java.security.NoSuchAlgorithmException, java.security.InvalidKeyException {
        // Convert counter to byte array
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte)(counter & 0xff);
            counter >>= 8;
        }
        
        // Generate HMAC-SHA1
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1");
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
}