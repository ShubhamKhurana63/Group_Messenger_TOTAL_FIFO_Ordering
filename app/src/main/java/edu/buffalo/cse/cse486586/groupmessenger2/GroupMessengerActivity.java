package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.database.Cursor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import android.net.Uri;
import java.util.*;
import android.telephony.TelephonyManager;
import android.content.Context;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */

 class QueueObject
{
    private String priortyProcess;
    private boolean isAgreed;
    private String message;
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPriortyProcess() {
        return priortyProcess;
    }
    public void setPriortyProcess(String priortyProcess) {
        this.priortyProcess = priortyProcess;
    }
    public boolean isAgreed() {
        return isAgreed;
    }
    public void setAgreed(boolean isAgreed) {
        this.isAgreed = isAgreed;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return "QueueObject{" +
                "priortyProcess='" + priortyProcess + '\'' +
                ", isAgreed=" + isAgreed +
                ", message='" + message + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}




 class QueueSorter implements Comparator<QueueObject> {

    @Override
    public int compare(QueueObject o1, QueueObject o2) {
        float num1 = Float.parseFloat(o1.getPriortyProcess());
        float num2 = Float.parseFloat(o2.getPriortyProcess());
        if (num1 > num2) {
            return 1;
        } else if (num1 < num2) {
            return -1;
        }
        return 0;
    }

}


public class GroupMessengerActivity extends Activity {


    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final Integer SERVER_PORT=10000;
    //volatile Integer dummyCheck=1;
    volatile boolean[] portFlagArray=new boolean[5];
    boolean isWipedDone=false;
    Map<String,String> ringMapper=new HashMap<String, String>();

    List<String> portsList=new ArrayList<String>();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        Arrays.fill(portFlagArray,Boolean.TRUE);

            portsList.add("11108");
            portsList.add("11112");
            portsList.add("11116");
            portsList.add("11120");
            portsList.add("11124");

        ringMapper.put("11108","11112");
        ringMapper.put("11112","11116");
        ringMapper.put("11116","11120");
        ringMapper.put("11120","11124");
        ringMapper.put("11124","11108");



        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        try
        {




            ServerSocket serverSocket=new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,serverSocket);



            TelephonyManager tel = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

            new RingHeartBeat().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,myPort);


            //spawning the server thread, as the application launches
        }catch (IOException ex)
        {
            ex.printStackTrace();
            Log.e(TAG, "Can't create a ServerSocket");
        }

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //send button impl
                        EditText editText=(EditText)findViewById(R.id.editText1);
                        String toBeSend=editText.getText().toString();
                        editText.setText("");
                        //multicasting message
                        new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,toBeSend);//client thread will be spawned, on each click
                    }
                }
        );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    @Override
    protected void onDestroy() {

        Log.e(" DESTROYED FINALLY","");

        super.onDestroy();
    }

    /**
     * The class for binding the avd's into heartbeat ring(working on ping-ack mechanism)
     * 0-1-2-3-4-0
     *Knowledge of the ring was grabbed from the course slide.
     */

    private class RingHeartBeat extends AsyncTask<String,Void,Void>
    {
        String []AVD_PORTS={"11108","11112","11116","11120","11124"};
        @Override
        protected Void doInBackground(String... strings) {

            List<String> localList=new ArrayList<String>();
            for(String str:AVD_PORTS)
            {
                localList.add(str);
            }

            try
            {
                Thread.sleep(10000);
            }catch(InterruptedException ex)
            {

            }

            String ownPort=strings[0];
            String sendingPort=ringMapper.get(ownPort);
            int clientId=Arrays.binarySearch(AVD_PORTS,sendingPort);
            boolean onceRemoved=false;
            while(true)
            {
                try
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(sendingPort));
                    socket.setSoTimeout(1500);
                    PrintWriter printWriter=new PrintWriter(socket.getOutputStream());
                    printWriter.print("ping "+clientId+"\n");
                    printWriter.flush();

                    BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String ack=bufferedReader.readLine();
                    if(ack!=null)
                    {
                        socket.close();
                    }
                }catch (SocketTimeoutException ex)
                {
                    Log.e(""+isWipedDone,"flag value");
                    if(!isWipedDone)
                    {
                        Log.e(""+localList.size(),"size of port list");
                        if(!onceRemoved)
                        {
                            localList.remove(clientId);
                            onceRemoved=true;
                        }

                        Log.e("clientId",">>>>>>>>>>"+clientId);
                        boolean flag=wipeOffFailedInstanceMessages(Integer.toString(clientId),localList,"pingmode");
                        Log.e("clientId","========WIPED FROM PING SPOT========"+flag);
                        //break;
                    }
                }catch (IOException ex)
                {
                    Log.e(""+isWipedDone,"flag value");
                    if(!isWipedDone)
                    {
                        Log.e(""+localList.size(),"size of port list");
                        if(!onceRemoved)
                        {
                            localList.remove(clientId);
                            onceRemoved=true;
                        }
                        boolean flag= wipeOffFailedInstanceMessages(Integer.toString(clientId),localList,"pingmode");
                        Log.e("clientId","========WIPED FROM PING SPOT========"+flag);
                        //break;
                    }
                }
                try
                {
                    Thread.sleep(5000);
                }catch(InterruptedException ex)
                {

                }
            }
        //return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket,String,Void>
    {

        //PriorityQueue<QueueObject> priorityQueue=new PriorityQueue<QueueObject>(10,new QueueSorter());

        PriorityQueue<QueueObject> priorityQueue=new PriorityQueue(10,new QueueSorter());
        Integer proposedNumber=0;

        /**
         *
         *Lines===================329-331======================================================================
         *The basic code has been taken from my PA1 and PA2-A submitted code.
         * The knowledge of priority queue was taken from the below link-:
         * https://www.geeksforgeeks.org/implement-priorityqueue-comparator-java/
         *https://stackoverflow.com/questions/4969760/setting-a-timeout-for-socket-operations( for server socket timeout)
         *
         */

        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            ServerSocket serverSocket = serverSockets[0];

            Context context=getApplicationContext();
            TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
            String []AVD_PORTS={"11108","11112","11116","11120","11124"};
            int processNumber=-1;
            for(int i=0;i<AVD_PORTS.length;i++)
            {
                if(myPort.equals(AVD_PORTS[i]))
                {
                    processNumber=i;
                    break;
                }
            }


                PrintWriter printWriter=null;
                BufferedReader bufferedReader=null;


                    while(true)//infinite loop ensures that server thread is always up and running
                    {
                        try
                        {
                            Socket socket=serverSocket.accept();//blocking call, where server waits for the client to establish connection & send data
                            InputStream is=socket.getInputStream();
                            OutputStream os=socket.getOutputStream();

                            bufferedReader=new BufferedReader(new InputStreamReader(is));//bufferedReader for reading from socket
                            printWriter=new PrintWriter(new OutputStreamWriter(os));//printwriter for writing on the socket

                            String receivedString=bufferedReader.readLine();//doing readline as an escape sequence of \n is at end
                            //Log.d(receivedString, " Received ===== String ");
                            //ping logic from server
                            if(receivedString.contains("ping"))
                            {
                                Log.e("PING RECEIVED","====");
                                printWriter.print("pingack"+"\n");
                                printWriter.flush();
                                continue;
                            }

                            //clearning off data from server
                            if(receivedString.contains("wipe: "))
                            {
                                boolean pingMode=true;
                                Log.e("WIPE STARTED","============================");
                                StringTokenizer tokenizer=new StringTokenizer(receivedString,"|");
                                tokenizer.nextToken();
                                String clientId=tokenizer.nextToken();
                                String mode=tokenizer.nextToken();
                                //if(mode.equals("pingmode"))
                                //{
                                    //pingMode=true;
                                //}

                                for(QueueObject object:priorityQueue)
                                {
                                    Log.e("==GEN QUEUE==",""+object.toString());
                                    if(!object.isAgreed() && object.getClientId().equals(clientId))
                                    {
                                        Log.e("==WIPE REMOVE==",""+object.toString());
                                        priorityQueue.remove(object);
                                    }
                                }
                                isWipedDone=true;
                                printWriter.print("ACK"+"\n");
                                printWriter.flush();
                                QueueObject obj=priorityQueue.poll();
                                if(obj!=null)
                                {
                                    priorityQueue.add(obj);
                                }

                                Uri mUri= mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                ContentResolver mContentResolver=getContentResolver();
                                while(pingMode && priorityQueue.size()>0 && priorityQueue.peek().isAgreed())
                                {
                                    ContentValues contentValues=new ContentValues();
                                    contentValues.put("key",""+seqCtr);
                                    QueueObject object=priorityQueue.poll();
                                    contentValues.put("values",object.getMessage());
                                    Log.e("======="+seqCtr+"=====",object.getMessage()+"|||"+object.getPriortyProcess()+"|||"+object.hashCode());
                                    mContentResolver.insert(mUri,contentValues);
                                    seqCtr++;
                                }
                                continue;
                            }

                            //proposals sending logic of server
                            if(receivedString.contains("get priority: "))//message tagging needed because, need to clear proposed messages
                            {
                                //Log.d(receivedString, " Received String ");

                                int priorityNumber=proposedNumber+1;
                                proposedNumber++;
                                String proposalProcess=priorityNumber+"."+processNumber;
                                StringTokenizer tokenizer=new StringTokenizer(receivedString,"|");
                                tokenizer.nextToken();
                                String message=tokenizer.nextToken();
                                QueueObject queueObject=new QueueObject();
                                queueObject.setAgreed(false);
                                queueObject.setClientId(tokenizer.nextToken());
                                queueObject.setMessage(message);
                                queueObject.setPriortyProcess(proposalProcess);
                                priorityQueue.add(queueObject);

                                printWriter.print(proposalProcess+"."+queueObject.hashCode()+"\n");
                                printWriter.flush();//flushing the data  in the stream
                            }else{
                                //final decisions
                                //String receivedString = values[0].trim();

                                StringTokenizer tokenizer=new StringTokenizer(receivedString,"|");

                                String part1=tokenizer.nextToken();
                                String part2=tokenizer.nextToken();

                                StringTokenizer priorityTokeinzer=new StringTokenizer(part2,".");

                                String agreedPriority=priorityTokeinzer.nextToken();
                                String agreedProcess=priorityTokeinzer.nextToken();
                                String hashCode=priorityTokeinzer.nextToken();

                                if(proposedNumber<Integer.parseInt(agreedPriority))
                                {
                                    proposedNumber=Integer.parseInt(agreedPriority);
                                }


                                QueueObject obj=priorityQueue.poll();
                                if(obj!=null)
                                {
                                    priorityQueue.add(obj);
                                }

                                for(QueueObject object:priorityQueue)
                                {
                                    if(Integer.toString(object.hashCode()).equals(hashCode))
                                    {
                                        object.setAgreed(true);
                                        object.setPriortyProcess(agreedPriority+"."+agreedProcess);
                                        //Log.e("===PRIORITY VALUE===",object.toString());
                                        break;
                                    }
                                }

                                 obj=priorityQueue.poll();
                                if(obj!=null)
                                {
                                    priorityQueue.add(obj);
                                }
                                //overwriting the priorities
                                publishProgress(receivedString);

                                Uri mUri= mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                ContentResolver mContentResolver=getContentResolver();
                                while(priorityQueue.size()>0 && priorityQueue.peek().isAgreed())
                                {
                                    ContentValues contentValues=new ContentValues();
                                    contentValues.put("key",""+seqCtr);
                                    QueueObject object=priorityQueue.poll();
                                    contentValues.put("values",object.getMessage());
                                    Log.e("======="+seqCtr+"=====",object.getMessage()+"|||"+object.getPriortyProcess()+"|||"+object.hashCode());
                                    mContentResolver.insert(mUri,contentValues);
                                    seqCtr++;
                                }
                                Log.e("=PRIORITY SIZE= ",""+priorityQueue.size());
                                String ackString="ACK";
                                printWriter.print(ackString+"\n");
                                printWriter.flush();
                            }
                            socket.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "=========reading data from socket failed====");
                        }catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                   }

            //return null;
        }

        /**
         * Lines================521-526=========================
         * The code has been taken from the onPTestClickListener.java
         * for buildUri method and the snippet for inserting data using content resolver.
         */

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }


        int seqCtr=0;
        @Override
        protected void onProgressUpdate(String... values) {

            String receivedString = values[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(receivedString + "\t\n");
        }
    }

    private int getClientId(String portString)
    {
        String []AVD_PORTS={"11108","11112","11116","11120","11124"};
        int clientId=Arrays.binarySearch(AVD_PORTS,portString);
        return clientId;
    }

    private synchronized boolean wipeOffFailedInstanceMessages(String failedClientId,List<String> activePortList,String mode)
    {
        String message="wipe: "+"|"+failedClientId+"|"+mode;
        int count=0;
        for(int i=0;i<activePortList.size();i++)
        {
            Log.e("inside WipeLogic",""+count+activePortList.size());
            try
            {
                String portToBeConnected=activePortList.get(i);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(portToBeConnected));
                socket.setSoTimeout(2000);
                BufferedReader bufferedReader=null;
                OutputStream os=null;
                InputStream is=null;
                if(socket.isConnected())
                {
                    os=socket.getOutputStream();
                    is=socket.getInputStream();
                    PrintWriter printWriter=new PrintWriter(os);
                    printWriter.print(message+"\n");
                    printWriter.flush();

                    bufferedReader=new BufferedReader(new InputStreamReader(is));
                    String ack=bufferedReader.readLine();
                    if(ack!=null && ack.equals("ACK"))
                    {
                        Log.e("==WIPE ACK PLACE==",""+count+activePortList.size());
                        count+=1;
                        socket.close();
                    }
                }

            }catch (SocketTimeoutException ex)
            {
                Log.e("inside socket timeout",""+count+activePortList.size());
                ex.printStackTrace();
            }
            catch (IOException ex)
            {
                Log.e("inside IO exception",""+count+activePortList.size());
                ex.printStackTrace();
            }
        }
        Log.e("PORT List==Message Size",""+count+activePortList.size());
        return count==activePortList.size();
    }

    /**
     * The basic constructs code below has been taken from PA2-A code
     */


    private class ClientTask extends AsyncTask<String,Void,Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            String msgToSend=strings[0];

            //String []AVD_PORTS={"11108","11112","11116","11120","11124"};//values of ports
            //String []AVD_PORTS={"11112"};
            String askString="get priority: ";
            float maxPriorityValue=Float.MIN_VALUE;
            List<String> objectHashList=new ArrayList<String>();
            List<Float> priorityContainer=new ArrayList<Float>();

            Context context=getApplicationContext();
            TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
            int clientId= getClientId(myPort);
            Log.d("====Client id ====",""+clientId);
            //asking for proposals from differennt processes
            for(int i=0;i<portsList.size();i++)
            {
                try
                {
                    String portToBeConnected=portsList.get(i);

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(portToBeConnected));//creating a socket connection for each port   //critical point
                    socket.setSoTimeout(2000);
                    BufferedReader bufferedReader=null;
                    OutputStream os=null;
                    InputStream is=null;
                    if(socket.isConnected())
                    {
                        os=socket.getOutputStream();
                        is=socket.getInputStream();
                        PrintWriter printWriter=new PrintWriter(os);
                        printWriter.print(askString+"|"+msgToSend+"|"+clientId+"\n");//writing the data on socket //critical point
                        printWriter.flush();
                        os.flush();
                        //ACK Receiving Logic
                        bufferedReader=new BufferedReader(new InputStreamReader(is));
                        String  priorityReceived=bufferedReader.readLine();//the format of the string received is |=====priority.process.hashcode======| //critical point
                       // Log.e(" running "+i+1,"===================RUN TIMES==============");
                        if(priorityReceived!=null)//null validation for the failures
                        {
                           StringTokenizer tokenizer=new StringTokenizer(priorityReceived,".");

                           String priorityProcess=tokenizer.nextToken()+"."+tokenizer.nextToken();
                           String objectHash=tokenizer.nextToken();
                            //Log.d("====getting seq ====",""+priority+"."+process);
                            priorityContainer.add(Float.parseFloat(priorityProcess));
                            Log.e(" value added ",""+priorityContainer.size());
                           /* if(Float.parseFloat(priorityProcess)>maxPriorityValue) {
                                 maxPriorityValue=Float.parseFloat(priorityProcess);
                            }*/
                            objectHashList.add(objectHash);//adding hashcode
                            socket.close();//closing socket, once the acknowledgement is received
                        }else
                        {
                            Log.e("","Inside else catch block");
                            throw new IOException();
                        }
                    }

                }catch(SocketTimeoutException ex)
                {
                    //Log.e("Getting inside socket timeout");
                    if(i+1==objectHashList.size())
                    {
                        objectHashList.remove(objectHashList.size()-1);
                    }

                    if(i+1==priorityContainer.size())
                    {
                        priorityContainer.remove(priorityContainer.size()-1);
                    }
                    String portFailed=portsList.get(i);
                    Log.e(portFailed,"=========PORT FAILED========");
                    int failedClientId=getClientId(portFailed);
                    portsList.remove(i);
                    i=i-1;
                    if(!isWipedDone)
                    {
                        boolean flag=wipeOffFailedInstanceMessages(Integer.toString(failedClientId),portsList,"wmode");
                        Log.e(""+flag,"===========DATA FINALLY WIPED1 for "+failedClientId+"========");
                    }


                    //Log.e(""+flag,"===========DATA WIPED1========");


                    //handling the logic for failures
                    //Log.e(ex.getMessage(),"===========FAILED1========");
                    //ex.printStackTrace();
                }catch(IOException ex)
                {
                    if(i+1==objectHashList.size())
                    {
                        objectHashList.remove(objectHashList.size()-1);
                    }

                    if(i+1==priorityContainer.size())
                    {
                        priorityContainer.remove(priorityContainer.size()-1);
                    }

                    String portFailed=portsList.get(i);
                    Log.e(portFailed,"=========PORT FAILED========");
                    int failedClientId=getClientId(portFailed);
                    portsList.remove(i);
                    i=i-1;
                    if(!isWipedDone)
                    {
                        boolean flag=wipeOffFailedInstanceMessages(Integer.toString(failedClientId),portsList,"wmode");
                        Log.e(""+flag,"===========DATA FINALLY WIPED1 for "+failedClientId+"========");
                    }
                   // Log.e(ex.getMessage(),"===========FAILED3========");
                }
            }


            //saved socket objects
            //https://stackoverflow.com/questions/36135983/cant-send-multiple-messages-via-socket
            Collections.sort(priorityContainer);
            maxPriorityValue=priorityContainer.get(priorityContainer.size()-1);

            Log.e(""+objectHashList.size(),"=======HASHLIST SIZE TRANSFERRED====="+portsList.size());

            //for sendinng the messages to the AVD's
            for(int i=0;i<portsList.size();i++)
            {
                BufferedReader bufferedReader=null;
                OutputStream os=null;
                InputStream is=null;
                try
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(portsList.get(i)));//establishing connections again
                    socket.setSoTimeout(2000);
                    if(socket.isConnected())
                    {
                        os=socket.getOutputStream();
                        is=socket.getInputStream();
                        PrintWriter printWriter=new PrintWriter(os);
                        //Log.e("",objectHashList.size()+"=======DaTA TAG====="+portsList.size());
                        String finalString=msgToSend+"|"+Float.toString(maxPriorityValue)+"."+objectHashList.get(i)+"\n";//final str being sent format-|==========message|priority.process.hashcode=========|
                        printWriter.print(finalString);//writing the data on socket
                        printWriter.flush();
                        //Log.d("====sending message====",""+msgToSend);
                        //ACK Receiving Logic
                        bufferedReader=new BufferedReader(new InputStreamReader(is));
                        String  priorityReceived=bufferedReader.readLine();
                        if(priorityReceived!=null && priorityReceived.equals("ACK"))
                        {
                            //Log.d("====post ack server====",""+priorityReceived);
                            socket.close();//closing socket, once the acknowledgement is received
                        }
                    }
                }catch(SocketTimeoutException ex)
                {
                    objectHashList.remove(i);
                    String portFailed=portsList.get(i);
                    portsList.remove(i);
                    int failedClientId=getClientId(portFailed);
                    Log.e(portFailed,"=========PORT FAILED========");
                    i=i-1;
                    if(!isWipedDone)
                    {
                        boolean flag=wipeOffFailedInstanceMessages(Integer.toString(failedClientId),portsList,"wmode");
                        Log.e(""+flag,"===========DATA FINALLY WIPED1 for "+failedClientId+"========");
                    }
                    //Log.e(ex.getMessage(),"===========FAILED1========");
                    //ex.printStackTrace();
                }catch(IOException ex)
                {
                    objectHashList.remove(i);
                    String portFailed=portsList.get(i);
                    portsList.remove(i);
                    Log.e(portFailed,"=========PORT FAILED========");
                    int failedClientId=getClientId(portFailed);
                    i=i-1;
                    if(!isWipedDone)
                    {
                        boolean flag=wipeOffFailedInstanceMessages(Integer.toString(failedClientId),portsList,"wmode");
                        Log.e(""+flag,"===========DATA FINALLY WIPED1 for "+failedClientId+"========");
                    } //Log.e(ex.getMessage(),"===========FAILED3========");
                    //ex.printStackTrace();
                }
            }

            return null;

            }


        }
    }


