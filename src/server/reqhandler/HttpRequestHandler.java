/**
 * Created with IntelliJ IDEA.
 * User: mushfekur
 * Date: 7/29/13
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */

package server.reqhandler;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpRequestHandler implements Runnable {
    private boolean isDebugOn = true;

    private String directoryName;
    private Socket connectionSocket;

    //response message header informations
    String currentDate, serverInfo, lastModification;
    String contentType, contentLength, connectionType;

    //private PrintWriter outStream;
    private BufferedReader inStream;
    private DataOutputStream outStream;

    public HttpRequestHandler(Socket connectionSocket) {
        directoryName = System.getProperty("user.dir");
        this.connectionSocket = connectionSocket;

        currentDate = "Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + '\n';
        serverInfo = "server: " + "Simple HTTP server" + " (Ubuntu/Linux)" + '\n';
        lastModification = "Last-Modified: ";
        contentType = "Content-Type: text/html; charset=UTF-8" + '\n';
        contentLength = "Content-Length: ";
        connectionType = "Connection: ";
    }

    @Override
    public void run() {
        try {
            outStream = new DataOutputStream(connectionSocket.getOutputStream());
            inStream = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            String reqHeader = inStream.readLine();

            if (isDebugOn) {
                System.out.println("User Request: " + reqHeader);
            }

            if (reqHeader.startsWith("GET")) {
                processGetRequest(reqHeader);
            } else if (reqHeader.startsWith("POST")) {
                processPostRequest(reqHeader);
            } else {
                PrintWriter outMsg = null;
                try {
                    outMsg = new PrintWriter(connectionSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (outMsg != null) {
                        outMsg.close();
                    }
                }
                if (outMsg != null) {
                    outMsg.println("Command Not Found!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                inStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    String[] getTokens(String reqHeader) {
        String[] tokens;
        String delimCharacters = "[ ]+";
        tokens = reqHeader.split(delimCharacters);
        return tokens;
    }

    public void processGetRequest(String reqHeader) {
        String[] tokens = getTokens(reqHeader);
        String path = directoryName + "/" + tokens[1];
        File file = new File(path);
        PrintWriter printWriter = null;

        try {
            printWriter = new PrintWriter(connectionSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists()) {
            //BufferedReader buffer = null;
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(path, "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                //writing raw data in socket
                if (raf != null) {
                    byte[] data = new byte[(int) raf.length()];
                    raf.readFully(data);
                    outStream.write(data);
                    outStream.flush();
                }

                if (isDebugOn) {
                    System.out.println("File sent!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (printWriter != null) {
                //sending 404 Not Found
                printWriter.print(notFoundHeader("close"));
                printWriter.close();
            }
        }
    }

    public void processPostRequest(String reqHeader) {
        //String[] tokens = getTokens( reqHeader );
        String reqLine = null;

        try {
            //sending 200 OK
            PrintWriter printWriter = new PrintWriter(connectionSocket.getOutputStream(), true);
            String lastModified = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            printWriter.print(okHeader(reqHeader.length(), lastModified, "close"));
            printWriter.close();

            //printing POST data on server side terminal
            do {
                reqLine = inStream.readLine();
                if (reqLine.contains("Content-Disposition:")) {
                    String delimChars = "[ =\\\"]+";
                    String[] fieldValues = reqLine.split(delimChars);

                    reqLine = inStream.readLine();
                    reqLine = inStream.readLine();
                    System.out.println(fieldValues[3] + ": " + reqLine);
                }
            } while (inStream.ready());

            if (isDebugOn) {
                System.out.println("POST Request Received!");
                System.out.println("POST Request Processing Complete!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String okHeader(int contentLenght, String lastModified, String connectionType) {
        String okMsg = "HTTP/1.1 200 OK" + '\n' +
                this.currentDate +
                this.serverInfo +
                this.lastModification + lastModified + '\n' +
                this.contentType +
                this.contentLength + contentLength + '\n' +
                this.connectionType + connectionType + '\n' + '\n';

        if (isDebugOn) {
            System.out.println(okMsg);
        }
        return okMsg;
    }

    private String notFoundHeader(String connectionType) {
        String notFoundMsg = "HTTP/1.1 404 Not Found" + '\n' +
                this.currentDate +
                this.serverInfo +
                this.contentType +
                this.connectionType + connectionType + '\n' + '\n' +
                "<html>\n" +
                "<head>\n" +
                "  <title>404 Not Found</title>\n" +
                "</head>\n" +
                "<body align='center'><h1>\n" +
                "  Error 404: Content Not Found!\n" +
                "</h1></body>\n" +
                "</html>\n";

        if (isDebugOn) {
            System.out.println(notFoundMsg);
        }
        return notFoundMsg;
    }
}