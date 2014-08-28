package com.example.asistencia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import android.net.NetworkInfo;


public class MainActivity extends Activity {

	private String cardNumber;
    public MifareClassic mfc;
    private NfcAdapter mNfcAdapter;
	public Calendar c = Calendar.getInstance();
    public PendingIntent mPendingIntent;
    public  IntentFilter[] mFilters;
 	public String[][] mTechLists; 	
 	int secCount;
 	
 	private TextView texto;
    
    private String nombre;
    private String apellido_pat;
    private String apellido_mat;
    private String empresa;
    private String dni="1111";
    private String cliente;
    public String fecha;
 	public Button boton_entrada;
 	public Button boton_salida;
 	private String data_archivo;
 	public boolean up_foto;
 	
 	public String ftp_ip="190.153.249.118";
    public String ftp_clave="tui2013";
    public String ftp_user="tui@architeq.cl";
    private String ws_url="http://www.chilemap.cl/marcacion_app/ws_data.php";
	private String estado_producto="1";
	public String ftp_path="load_marca/";
    public String ftp_path_full="tui/load_marca/";
    public Boolean estado_conec=true;
 	public boolean no_conec=true;
 	/*CAMERa*/
 	private ImageView imagen;
	private static int TAKE_PICTURE = 1;
	private String name2="";
	private String name = "";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inicio);
		String[] archivos = fileList();
        
		if (!existe(archivos, "marca.txt"))
		{
			grabar("","marca.txt");
		}
		//leerArchivo("marca.txt");
		//LimpiarArchivo("marca.txt");
		if(!isInternetOn())
		{
			if(!no_conec)
			{
				setContentView(R.layout.inicio);
			}
			Toast.makeText(this, " SISTEMA NO CONECTADO A INTERNET. ", Toast.LENGTH_LONG).show();
			
		}
		/* FTP*/
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
        .permitAll()
        .build();   
		StrictMode.setThreadPolicy(policy);
		/**/
		/*NFC*/
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) 
		{
			Toast.makeText(this, "El equipo no soporta NFC.", Toast.LENGTH_LONG).show();
	        finish();
	        return;
	    }
		mPendingIntent = PendingIntent.getActivity(this, 0,
	                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
      
	     try {
	            ndef.addDataType("*/*");
	      } catch (MalformedMimeTypeException e) {
	           throw new RuntimeException("fail", e);
	      }
	      mFilters = new IntentFilter[] {
	                ndef,
	      };
	       mTechLists = new String[][] { new String[] { MifareClassic.class.getName() } };
	       //data_archivo=readArchivo();
	   		
	       
	       /*Fin nfc*/  
	       

	}
	
	
	 public void onResume() {
         super.onResume();
         mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
     }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	public byte[] readSeccionBloque(MifareClassic mfc2, int j, int bloque)
	{
		//byte[] key = {(byte)0xA0,(byte)0xA1,(byte)0xA2,(byte)0xA3,(byte)0xA4,(byte)0xA5};
		byte[] data = null;
		try
		{
			
		 //boolean auth2 = mfc2.authenticateSectorWithKeyA(j,key);
		 boolean auth2 = mfc2.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
		 if(auth2)
		 {
	   	 
	        //int bCount = mfc2.getBlockCountInSector(j);
	        int bIndex = 0;
	        bIndex = mfc2.sectorToBlock(j);      
	                       	 
	        data = mfc2.readBlock(bIndex+bloque);
		 }
	   
		}catch(Exception e){
			
		}
		return data;
	}
    public void writeText(MifareClassic mfc2, int j, int bloque, String dato)
    {
    	
    	//Toast.makeText(this, "paso 1", Toast.LENGTH_SHORT).show();
    	boolean auth2 = false;
    	 
    	int bIndex;
    	try
    	{    	 	
    	 	
    		auth2=mfc2.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
             if(auth2){
            	 //Toast.makeText(this, "paso 2 : "+dato.getBytes(), Toast.LENGTH_SHORT).show();
                 // 6.2) In each sector - get the block count
                 //int bCount = mfc2.getBlockCountInSector(j);                
                 bIndex = mfc2.sectorToBlock(j);             

                 //for(int i = 0; i < 3; i++){
                 byte[] value  = dato.getBytes();
                 byte[] toWrite = new byte[MifareClassic.BLOCK_SIZE];        

                 for (int i=0; i<MifareClassic.BLOCK_SIZE; i++) {
                       if (i < value.length) toWrite[i] = value[i];
                       else toWrite[i] = 0;
                 }         
                 mfc2.writeBlock(bIndex+bloque, toWrite);
                 //mfc2.writeBlock(bIndex+bloque, new byte[] { 'r', 'o','s', 'a', 's', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',' ', ' ', ' ' });
                 showMensaje("escribio dato"+dato);
             	 //Toast.makeText(this, "paso 3", Toast.LENGTH_SHORT).show();
                 //}
               
             }else{ // Authentication failed - Handle it
            	 showMensaje("no accedio a escribir");           	 
            	
             }
       	 
        
    	}catch(Exception e){
    		//byte[] ejemplo=valor.getBytes();
    		showMensaje("cayo"+e.getMessage());
    		
    	}
    }
    public void handleIntent(Intent intent) 
    {
  	  //Toast toast = Toast.makeText(this, "Leyendo datos...", Toast.LENGTH_SHORT);
  	  //toast.show();
  	  
  	   String action = intent.getAction();
       
       if (mNfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) 
       {
       	
           Tag tagFromIntent = intent.getParcelableExtra(mNfcAdapter.EXTRA_TAG);
           ByteArrayToLong(tagFromIntent.getId());
           bin2hex(tagFromIntent.getId());
           
           byte[] data;
           mfc = MifareClassic.get(tagFromIntent);
           //Toast.makeText(this, "paso :"+TagId_byte, Toast.LENGTH_LONG).show();
           try {
               mfc.connect();
               secCount = mfc.getSectorCount();
               byte[] data2;
               //showMensaje(""+secCount+"");
               data2=readSeccionBloque(mfc, 1, 0);
               nombre=getHexaString(data2).trim();
                             
               data2=readSeccionBloque(mfc, 1, 2);
               apellido_mat=getHexaString(data2).trim();
               
               data2=readSeccionBloque(mfc, 1, 1);
               apellido_pat=getHexaString(data2).trim();
               
               data2=readSeccionBloque(mfc, 2, 1);
               empresa=getHexaString(data2).trim();
               
               data2=readSeccionBloque(mfc, 2, 2);
               dni=getHexaString(data2).trim();
               
               data2=readSeccionBloque(mfc, 2, 0);
               cliente=getHexaString(data2).trim();
               //showMensaje("DATA::"+nombre+"");
               /*writeText(mfc, 1, 0, "vale");
               writeText(mfc, 1, 1, "munoz");
               writeText(mfc, 1, 2, "");
              
               writeText(mfc, 2, 0, "1");
               writeText(mfc, 2, 1, "Architeq");
               writeText(mfc, 2, 2, "156666947");*/

           } catch (IOException e) {
               //Log.e(TAG, "No Conecto", e);
           } finally {
               if (mfc != null) {
                  try {
                      mfc.close();
                  }
                  catch (IOException e) {
                 //     Log.e(TAG, "Error closing tag...", e);
                  }
               }
           }
           
       }
       
      

      
  }
    @Override
    public void onNewIntent(Intent intent) {
        //Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
    	//refreshFecha_hora();
		if(!isInternetOn() && !no_conec)
		{
			
			Toast.makeText(this, " SISTEMA NO CONECTADO A INTERNET. ", Toast.LENGTH_LONG).show();
		}else
		{
			
			handleIntent(intent);
			if(!dni.equals("") && !nombre.equals("") && !cliente.equals(""))
			{
				mostrarDatosTarjeta();
			}else
			{
				setContentView(R.layout.inicio);
				showMensaje("ERROR EN LA LECTURA DE LA TARJETA");
				
			}
			
		}
    	//leerTarjeta();
        
        
    }
    
    
	private static long ByteArrayToLong(byte[] inarray) {

        long resultado = inarray[0] & 0xff;

        for (int i = 1; i <= 3; i++) {
            resultado = (long) (resultado + ((inarray[i] & 0xff) * Math.pow(2,
                    (8 * i))));
        }
        return resultado;
    }
    private static String bytesToString(byte[] ary) {
		final StringBuilder result = new StringBuilder();
		for(int i = 0; i < ary.length; ++i) {
			result.append(Character.valueOf((char)ary[i]));
		}
		return result.toString();
	}
	    private String getHexaString(byte[] data) {
	    	
			cardNumber = bytesToString(data);
			return cardNumber; 
		}
	    private String parseCardNumber(byte[] data) {
	    	
			final byte[] number = Arrays.copyOfRange(data, 0, 12);
			cardNumber = bytesToString(number);
			return cardNumber; 
		}
	    
		static String bin2hex(byte[] data) {
		    return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
		}
		public void showMensaje(String texto)
		{
			Toast.makeText(this, texto, 20).show();
		}
		public void leerTarjeta()
		{
			Intent intent = getIntent();
		    handleIntent(intent); 	
		}
		public void refreshFecha_hora()
		{
			texto = (TextView) findViewById(R.id.textView1);
			
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			fecha = sdf.format(cal.getTime());
			
			
	    	texto.setText("RELOJ : "+fecha);
		}
		public void mostrarDatosTarjeta()
		{
			name2="";
			name = "";
			setContentView(R.layout.activity_main);
			
			refreshFecha_hora();
			texto = (TextView) findViewById(R.id.textView3);
			texto.setText(empresa.toUpperCase());
			texto = (TextView) findViewById(R.id.textView4);
			texto.setText(nombre+" "+apellido_pat+" "+apellido_mat);
			texto = (TextView) findViewById(R.id.textView5);
			texto.setText(dni);
			
			boton_entrada = (Button) findViewById(R.id.button1);
			boton_entrada.setOnClickListener(new View.OnClickListener()
		    {
		        @Override
		        public void onClick(View v) {
		        	
		        	
		        	Intent intent_foto =  new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		        	
                	int code = TAKE_PICTURE;
		        	Date date = new Date();
	            	SimpleDateFormat hourFormat = new SimpleDateFormat("HHmmss");
	            	name = Environment.getExternalStorageDirectory() + "/"+dni+"_"+hourFormat.format(date)+".jpg";
	            	name2=dni+"_"+hourFormat.format(date)+".jpg";
	            	Uri output = Uri.fromFile(new File(name));
	            	
	                intent_foto.putExtra(MediaStore.EXTRA_OUTPUT, output);
	            	//intent_foto.putExtra(MediaStore.EXTRA_SIZE_LIMIT,2000);
	            	//
	        	    startActivityForResult(intent_foto, code);     
	        	    
		        }
		     });
			boton_salida = (Button) findViewById(R.id.button2);
			boton_salida.setOnClickListener(new View.OnClickListener()
		    {
		        @Override
		        public void onClick(View v) {
		        	
		        	marcacion(2);
	        	    
		        }
		     });
		}
		
	  	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	  		
	  		//showMensaje(""+requestCode+"");
		   	if (requestCode == TAKE_PICTURE) {
	    		/**
	    		 * Si se reciben datos en el intent tenemos una vista previa (thumbnail)
	    		 */
	    		if (data != null) {
	    			/**
	    			 * En el caso de una vista previa, obtenemos el extra ÒdataÓ del intent y 
	    			 * lo mostramos en el ImageView
	    			 */
	    			if (data.hasExtra("data")) { 
	    				ImageView iv = (ImageView)findViewById(R.id.imageView1);
	    				iv.setImageBitmap((Bitmap) data.getParcelableExtra("data"));
	    			}
	    		/**
	    		 * De lo contrario es una imagen completa
	    		 */    			
	    		} else {
	    			/**
	    			 * A partir del nombre del archivo ya definido lo buscamos y creamos el bitmap
	    			 * para el ImageView
	    			 */
	    			
	    			imagen = (ImageView)findViewById(R.id.imageView1);
	    			imagen.setImageBitmap(BitmapFactory.decodeFile(name));
	    			imagen.setVisibility(View.VISIBLE);
	    			marcacion(1);
	    			
	    			/**
	    			 * Para guardar la imagen en la galer’a, utilizamos una conexi—n a un MediaScanner
	    			 */
	    			new MediaScannerConnectionClient() {
	    				private MediaScannerConnection msc = null; {
	    					
	    					msc = new MediaScannerConnection(getApplicationContext(), this); msc.connect();
	    				}
	    				public void onMediaScannerConnected() { 
	    					msc.scanFile(name, null);
	    				}
	    				public void onScanCompleted(String path, Uri uri) { 
	    					msc.disconnect();
	    				} 
	    			};	
	    			
	    		}
	    	/**
	    	 * Recibimos el URI de la imagen y construimos un Bitmap a partir de un stream de Bytes
	    	 */
	    	} 
		}

	  	 public void onPause() {
	          super.onPause();
	          mNfcAdapter.disableForegroundDispatch(this);
	      }
	  	 public void marcacion(int tipo)
	  	 {
	 		if(!isInternetOn() && !no_conec)
			{
	 			if(!no_conec)
	 			{
	 				setContentView(R.layout.inicio);
	 				Toast.makeText(this, " SISTEMA NO CONECTADO A INTERNET. ", Toast.LENGTH_LONG).show();
	 			}
				
			}else
			{
				
	 		Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			fecha = sdf.format(cal.getTime());
			
	  		String myUrl=ws_url+"?tipo=1&tipo_m="+tipo+"&imagen="+name2+"&cli="+cliente+"&nombre="+nombre+"&app="+apellido_pat+"&apm="+apellido_mat+"&dni="+dni+"&fecha="+fecha+"";

	  		try
	  		{
	  			estado_producto = DownloadText(myUrl.replaceAll(" ", "%20"));
	  		}catch (Exception e)
	        {
	            e.printStackTrace();	            
	            
	            if(!no_conec)
	            {
	            	setContentView(R.layout.inicio);
	            	showMensaje("NO SE PUDO REALIZAR LA MARCACION. POR FAVOR INTENTELO NUEVAMENTE ");
	            }
				//checkInternet();
	            //almacenar en disco
	            
	        }
			if(estado_producto.equals("0"))
			{
				if(tipo==1)
				{
					up_foto=true;
					subirFtp(name,ftp_path_full,name2,0);
				}
				setContentView(R.layout.inicio);	
				showMensaje("MARCACION REALIZADA");
			}else
			{
				if(!no_conec)
				{
					setContentView(R.layout.inicio);
					showMensaje("NO SE PUDO REALIZAR LA MARCACION. POR FAVOR INTENTELO NUEVAMENTE ");
				}
					
					grabar(nombre+"|"+apellido_pat+"|"+apellido_mat+"|"+tipo+"|"+dni+"|"+fecha+"|"+cliente+"|"+name2,"marca.txt");
					setContentView(R.layout.inicio);	
					showMensaje("MARCACION REALIZADA");
			   
			   //checkInternet();
			}
			}
	  	 }
		public void subirFtp(String archivo, String path_ftp,String archivo_final, int tipo){
				
		        FTPClient con = null;


		        try
		        {
		        	
		        	
		            con = new FTPClient();  
		            con.setConnectTimeout(5000);
		            
	            	
		            con.connect(ftp_ip);
		            Context fileContext;
		            fileContext = this.getBaseContext();
		            		
		            if (con.login(ftp_user, ftp_clave))
		            {
		            	
		            	con.setSoTimeout(5000);
		            	//con.setDataTimeout(5);
		                con.enterLocalPassiveMode(); // important!
		                con.setFileType(FTP.BINARY_FILE_TYPE);
		                
		                boolean result=false;
		                
		                
		                if(tipo==0)
		                {
		                	FileInputStream in = new FileInputStream(new File(archivo));
		                	result = con.storeFile(""+ftp_path+""+archivo_final+"", in);
		                	in.close();
		                	
		                }
		                if(tipo==1)
		                {
		                	
		                	FileInputStream srcFileStream = fileContext.openFileInput(archivo);
		                	result = con.storeFile(""+ftp_path+""+archivo_final+"", srcFileStream);
		                	srcFileStream.close();
		                }
		                
		                
		                if (result) Log.v("upload result", "succeeded");
		                
		                con.logout();
		                con.disconnect();
		            }
		        }
		        catch (Exception e)
		        {
		            e.printStackTrace();	        
		            up_foto=false;
		            Toast.makeText(this, "Problemas con la conexion a internet.", Toast.LENGTH_SHORT).show();
		            if(!no_conec)
		            {
		            	
		            }
		            //almacenar en disco
		        }
		    }
		
		 private String DownloadText(String URL)
		    {
		        int BUFFER_SIZE = 2000;
		        InputStream in = null;
		        try {
		            in = OpenHttpConnection(URL);
		        } catch (IOException e1) {
		            // TODO Auto-generated catch block
		            e1.printStackTrace();
		            if(!no_conec)
		            {
		            	
		            }
		            //Toast.makeText(this, "Problemas con la conexion al:: "+e1.getMessage(), Toast.LENGTH_SHORT).show();
		            return "";
		        }
		         
		        InputStreamReader isr = new InputStreamReader(in);
		        int charRead;
		        String str = "";
		        char[] inputBuffer = new char[BUFFER_SIZE];          
		        try {
		        	//Toast.makeText(this, "Conecto.", Toast.LENGTH_SHORT).show();
		            while ((charRead = isr.read(inputBuffer))>0)
		            {                    
		                //---convert the chars to a String---
		                String readString = String.copyValueOf(inputBuffer, 0, charRead);
		                str += readString;
		                inputBuffer = new char[BUFFER_SIZE];
		            }
		            //Toast.makeText(this, "encontro."+str, Toast.LENGTH_SHORT).show();
		            
		            in.close();
		            
		           
		        } catch (IOException e) {
		        	
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		            //Toast.makeText(this, "Problemas con la conexion 2:: ."+e.getMessage(), Toast.LENGTH_SHORT).show();
		            if(!no_conec)
		            {
		            	
		            }
		            return "";
		        }    
		        
		        return str;        
		    }
		    private InputStream OpenHttpConnection(String urlString) 
		    	    throws IOException
		    	    {
		    	        InputStream in = null;
		    	        int response = -1;
		    	                
		    	        URL url = new URL(urlString); 
		    	        URLConnection conn = url.openConnection();
		    	                  
		    	        if (!(conn instanceof HttpURLConnection))                     
		    	            throw new IOException("Not an HTTP connection");
		    	         
		    	        try{
		    	            HttpURLConnection httpConn = (HttpURLConnection) conn;
		    	            httpConn.setAllowUserInteraction(false);
		    	            httpConn.setInstanceFollowRedirects(true);
		    	            httpConn.setRequestMethod("GET");
		    	            httpConn.setConnectTimeout(5000);
		    	            httpConn.connect(); 
		    	 
		    	            response = httpConn.getResponseCode();                 
		    	            if (response == HttpURLConnection.HTTP_OK) {
		    	                in = httpConn.getInputStream();                                 
		    	            }                     
		    	        }
		    	        catch (Exception ex)
		    	        {
		    	        	if(!no_conec)
		    	        	{
		    	        		Toast.makeText(this, "Problemas con la conexion de internet. Por favor solicitar revisar.", Toast.LENGTH_SHORT).show();
		    	        	}
		    	        	//Toast.makeText(this, "Problemas "+ex.getMessage(), Toast.LENGTH_SHORT).show();
		    	        }
		    	        return in;     
		    	    }
		    public boolean isInternetOn() {
		         
		    	ConnectivityManager cm =
		    	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		    	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
		    	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
		    	        return true;
		    	    }
		    	    return false;
		        }
    	    private boolean existe(String[] archivos, String archbusca) {
		    			for (int f = 0; f < archivos.length; f++)
		    				if (archbusca.equals(archivos[f]))
		    					return true;
		    			return false;
		    		}

    	    public String readArchivo(String archivo_nom)
    	    {
    	    	
    	        String[] archivos = fileList();
    	        String todo = "";
    			if (existe(archivos, archivo_nom))
    				try {
    					InputStreamReader archivo = new InputStreamReader(
    							openFileInput(archivo_nom));
    					BufferedReader br = new BufferedReader(archivo);
    					String linea = br.readLine();
    					
    					while (linea != null) {
    						todo = todo + linea + "\n";
    						linea = br.readLine();
    					}
    					br.close();
    					archivo.close();
    					
    				} catch (IOException e) {
    				}
    	       return todo;
    	    }
    		public void grabar(String datos,String archivo_nom) 
    		{
    			
    			data_archivo=readArchivo("marca.txt");
    			try {
    				
    				OutputStreamWriter archivo = new OutputStreamWriter(openFileOutput(
    						archivo_nom, Activity.MODE_PRIVATE));	    				
    			   	
    				
    				archivo.write(data_archivo+""+datos.toString());
    				archivo.flush();
    				archivo.close();
    			} catch (IOException e) {
    			}
    			//Toast t = Toast.makeText(this, "Los datos fueron grabados en archivo.",
    				//	Toast.LENGTH_SHORT);
    			//t.show();
    			//finish();
    			
    		}
    		public void LimpiarArchivo(String archivo_nom) {
   			 
    			try {
    				OutputStreamWriter archivo = new OutputStreamWriter(openFileOutput(
    						archivo_nom, Activity.MODE_PRIVATE));
    				archivo.write("");
    				archivo.flush();
    				archivo.close();
    			} catch (IOException e) {
    			}
    			
    		}
    		public void leerArchivo(String archivo_nom)
    		{
    			String archivo=readArchivo(archivo_nom);
		   		
		   		String data_arr[] =archivo.split("\n");
		   		
		   		
		   		int i=0;
	        	for(i=0;i<data_arr.length;i++)
	        	{
	        		showMensaje("num:"+i+""+data_arr[i]);
	        		
	        	}
    		}
}
