/**
 * Created with IntelliJ IDEA.
 * User: mushfekur
 * Date: 7/29/13
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */

package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    public static void main(String[] args) {
        final int portNo = 8080;
        //boolean isDebugOn = true;

        Socket connectionSocket = null;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portNo);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final int corePoolSize = 0;
        final int MaxPoolSize = 9;
        final int keepAliveTime = 1;
        ThreadPoolExecutor threadPool = null;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(10);

        threadPool = new ThreadPoolExecutor(corePoolSize, MaxPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);

        while (true) {
            System.out.println("Waiting for request...");
            try {
                //serverSocket = new ServerSocket( portNo );
                if (serverSocket != null) {
                    connectionSocket = serverSocket.accept();
                }
                System.out.println("Request Accepted...");
                HttpRequestHandler requestHandler = new HttpRequestHandler(connectionSocket);
                Thread thread = new Thread(requestHandler);

                if (threadPool.getPoolSize() < MaxPoolSize) {
                    threadPool.execute(thread);
                } else {
                    System.out.println("server too busy!" + '\n');
                    thread.interrupt();
                }
                /*
                if ( isDebugOn ) {
                	System.out.println("Thread Pool Size : " + threadPool.getPoolSize());
                }
                */
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}