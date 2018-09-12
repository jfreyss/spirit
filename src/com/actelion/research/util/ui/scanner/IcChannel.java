/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
 * CH-4123 Allschwil, Switzerland.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.util.ui.scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the communication layer between the application and the Fluidx Reader
 * it is using TCP socket communication
 *
 * The response of the Reader Intellicode software essentially have a start/middle & end. The sequence is..
 *      1) ack.  Command is prefixed with ack: to acknowledge the command. E.g, ack:list acknowldges the list command
 *      2) msg: During processing,  messages are recieved with this prefix
 *      3) warning: During processing,  warning messages are recieved with this prefix
 *      4) success: Command is prefixed with sucess: when the command was successful. E.g, success:list for a successful list command
 *      5) fail: Command is prefixed with fail: when the command failed. E.g, fail:list for a failed list command
 *
 * @author Karim Mankour
 */
public class IcChannel {
    private Socket clientSocket;
    private String ipAddress;
    private int port = 8001;
    private CmdState cmdState = CmdState.UNKNOWN;
    private List<String> response = new ArrayList<>();

    public enum CmdState {
        UNKNOWN,
        WAITING_ACK,
        PROCESSING,
        SUCCESS,
        FAIL
    }

    /**
     * Constructor that is defaulted the host ip address to the localhost one
     */
    public IcChannel() {
        try {
            this.ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends Intellicode command passed as a parameter to the Intellicode driver
     * that is listening on a specific port number
     * @param command
     */
    public void send(String command) {
        try {
            Thread.sleep(2000);
            cmdState = CmdState.WAITING_ACK;
            clientSocket = new Socket(ipAddress, port);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // write command and read response
            out.println(command);

            while ( cmdState != CmdState.SUCCESS && cmdState != CmdState.FAIL ) {
                updateResponse(in.readLine());
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public CmdState getCmdState() {
        return cmdState;
    }

    public List<String> getResponse() {
        return this.response;
    }

    private void updateResponse(String msg) {
        msg.trim();

        switch (cmdState) {
            case WAITING_ACK: {
                if (msg.startsWith("ack:") == true) {
                    response.clear();
                    cmdState = CmdState.PROCESSING;
                }
                break;
            }
            case PROCESSING: {
                if (msg.startsWith("msg:") == true) {
                    response.add(msg.substring(4));
                } else if (msg.startsWith("warning:") == true) {

                } else if (msg.startsWith("success:") == true) {
                    cmdState = CmdState.SUCCESS;
                } else if (msg.startsWith("fail:") == true) {
                    cmdState = CmdState.FAIL;
                } else {
                    response.add(msg);
                }
                break;
            }
            default:
                break;
        }
    }
}
