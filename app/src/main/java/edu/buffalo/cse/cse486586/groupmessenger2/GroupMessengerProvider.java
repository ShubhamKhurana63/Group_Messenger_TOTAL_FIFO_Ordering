package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        Log.v("insert", values.toString());
       // return uri;
        Context context=getContext();
        Set<String> keySet=values.keySet();
        Stack<String> stack=new Stack<String>();
        for(String s:keySet)
        {
            stack.push((String)values.get(s));
        }
        String fileName=stack.pop();
        String value=stack.pop();
        File file=context.getFileStreamPath(fileName);
        //Log.d("",context.getFilesDir().toString());
        FileWriter fileWriter=null;
        try {
            try
            {
                if(!file.exists())//if file does not exists then at that point creating new one, else overwriting
                {
                    file=new File(context.getFilesDir(),fileName);
                }
                fileWriter=new FileWriter(file);
                fileWriter.write(value+"\n");
                fileWriter.flush();
            }finally {
                //Log.d("<===FILE STATUS======>",""+file.exists());
                fileWriter.close();
            }
        }catch (IOException ex) {
            ex.printStackTrace();
        }
        return uri;

    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);

        Context context=getContext();
        File file=context.getFileStreamPath(selection);
        String [] colNames= {"key","value"};
        MatrixCursor matrixCursor=new MatrixCursor(colNames);
        FileReader fileReader=null;
        if(file.exists())//checking if the local file exists
        {
            try
            {
                try
                {
                    fileReader=new FileReader(file);
                    BufferedReader bufferedReader=new BufferedReader(fileReader);
                    String value=bufferedReader.readLine();
                    String[] array=new String[2];
                    array[0]=selection;
                    array[1]=value;
                    matrixCursor.addRow(array);
                }finally {
                    fileReader.close();
                }
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        //Log.v("query", selection);
        return matrixCursor;
    }
}
