/**
 * 
 */
package com.lescarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is a utility class that takes KUVO txt and m3u8 playlist data from Rekordbox
 * and turns it into the XML format used by Rekordbox 5 and the JSON format used by Tribe XR.
 * <br/><br/>
 * This has the following limitations:<br/>
 * <ul><li>Starting positions in tracks aren't carried over</li>
 * <li>A number of fields not required for use by Tribe XR are left blank or defined with a static default value</li>
 * </ul>
 * @author Les Carter
 *
 */
public class PlaylistGenerator {

	public static final String PARAM_SRCDIR="pg.srcdir";
	public static final String PARAM_DESTDIR="pg.destdir";
	public static final String PARAM_PLAYLIST="pg.playlist";
	public static final String PARAM_ISJSON="pg.isjson";
	public static final String PARAM_ISXML="pg.isxml";
	
	public static final String STR_DJ_LIST="DJ_PLAYLISTS";
	public static final String STR_VERSION="Version";
	public static final String STR_VERSION_NUM="1.0.0";
	public static final String STR_PRODUCT="PRODUCT";
	public static final String STR_NAME="Name";
	public static final String STR_PRODUCT_NAME="PlaylistGenerator";
	public static final String STR_PRODUCT_VERSION="1.0";
	public static final String STR_COMPANY="Company";
	public static final String STR_PRODUCT_COMPANY="PlaylistGenerator";
	public static final String STR_COLLECTION="COLLECTION";
	public static final String STR_ENTRIES="Entries";
	public static final String STR_TRACK="TRACK";
	public static final String STR_TRACKID="TrackID";
	public static final String STR_ARTIST="Artist";
	public static final String STR_COMPOSER="Composer";
	public static final String STR_ALBUM="Album";
	public static final String STR_GROUPING="GROUPING";
	public static final String STR_GENRE="GENRE";
	public static final String STR_KIND="Kind";
	public static final String STR_KIND_MP3="MP3 File";
	public static final String STR_KIND_WAV="WAV File";
	public static final String STR_SIZE="Size";
	public static final String STR_TOTALTIME="TotalTime";
	public static final String STR_DISCNUMBER="DiscNumber";
	public static final String STR_TRACKNUMBER="TrackNumber";
	public static final String STR_YEAR="Year";
	public static final String STR_AVERAGEBPM="AverageBPM";
	public static final String STR_DATEADDED="DateAdded";
	public static final String STR_BITRATE="BitRate";
	public static final String STR_SAMPLERATE="SampleRate";
	public static final String STR_COMMENTS="Comments";
	public static final String STR_PLAYCOUNT="PlayCount";
	public static final String STR_RATING="Rating";
	public static final String STR_LOCATION="Location";
	public static final String STR_REMIX="Remixer";
	public static final String STR_TONALITY="Tonality";
	public static final String STR_LABEL="Label";
	public static final String STR_MIX="Mix";
	public static final String STR_PLAYLISTS="PLAYLISTS";
	public static final String STR_NODE="NODE";
	public static final String STR_TYPE="Type";
	public static final String STR_COUNT="Count";
	public static final String STR_KEYTYPE="KeyType";
	public static final String STR_KEY="Key";
	public static final String STR_M3U8_SEPARATOR=" - ";
	
	public static final String STR_DEFAULT_SRCDIR=System.getProperty("user.dir");
	public static final String STR_DEFAULT_DESTDIR=System.getProperty("user.dir");
	public static final String STR_DEFAULT_ISXML=Boolean.FALSE.toString();
	public static final String STR_DEFAULT_ISJSON=Boolean.TRUE.toString();
	public static final String STR_DEFAULT_BITRATE="320";
	public static final String STR_DEFAULT_SAMPLERATE="44100";
	public static final String STR_DEFAULT_DISCNUMBER="0";	
	public static final String STR_DEFAULT_TRACKNUMBER="0";	
	public static final String STR_DEFAULT_METRO="4/4";
	public static final String STR_DEFAULT_INZIO="0.0";
	
	public static final String STR_EXTINF="#EXTINF:";
	
	public static final String STR_M3U8_FILE_EXTENSION=".m3u8";
	public static final String STR_TXT_FILE_EXTENSION=".txt";
	public static final String STR_MP3_FILE_EXTENSION=".mp3";
	public static final String STR_WAV_FILE_EXTENSION=".wav";
	public static final String STR_XML_FILE_EXTENSION=".xml";
	public static final String STR_JSON_FILE_EXTENSION=".json";
	
	public static final int INDEX_TRACKNUM=0;
	public static final int INDEX_ALBUM=4;
	public static final int INDEX_GENRE=5;
	public static final int INDEX_BPM=6;
	public static final int INDEX_KEY=9;
	public static final int INDEX_DATEADDED=10;
	
	protected static String cmdSrcDir=System.getProperty(PARAM_SRCDIR, STR_DEFAULT_SRCDIR);
	protected static String cmdDestDir=System.getProperty(PARAM_DESTDIR, STR_DEFAULT_DESTDIR);
	protected static String cmdPlaylist=System.getProperty(PARAM_PLAYLIST,null);
	protected static boolean cmdIsXML=Boolean.parseBoolean(System.getProperty(PARAM_ISXML,STR_DEFAULT_ISXML));
	protected static boolean cmdIsJSON=Boolean.parseBoolean(System.getProperty(PARAM_ISJSON,STR_DEFAULT_ISJSON));
	
	/**
	 * This application is used to generate Tribe XR JSON and Rekordbox 5 XML playlist files from the m3u8 and KUVO txt playlist formats exported by newer
	 * versions of Rekord box.  By default the application asks for the directory and the filename of the playlist (without the <code>.m3u8</code>
	 * or <code>.txt</code> extensions.  The <code>.m3u8</code> file must be present for either JSON or XML output as it contains the file location needed.  
	 * Both the <code>.m3u8</code> and <code>.txt</code> files must be present to produce the Rekordbox 5 XML output, and they should both have the same filename prefix.
	 * @param args the first element should contain the directory of where the source files are, the second element should contain the filename (without extension) of the file(s) to be processed.
	 */
	public static void main(String[] args) 
	{
		if(args.length>0)
		{
			System.out.println("Usage: PlaylistGenerator -D"+PARAM_SRCDIR+"=<srcdir> "
					+ "-D"+PARAM_DESTDIR+"=<destdir> "
					+ "-D"+PARAM_PLAYLIST+"=<playlist> "
					+ "-D"+PARAM_ISXML+"<isxml> "
					+ "-D"+PARAM_ISJSON+"<isjson>\n"
					+ "PlaylistGenerator -help [displays this help]\n\n"
					+ "srcdir - the source directory containing the playlist(s) exported from Rekordbox (default is current directory)\n"
					+ "destdir - the destination directory to put the transformed playlist (default is current directory)\n"
					+ "playlist - the parameter to specify a single playlist file (without the extension) instead of processing all playlists in the source directory (default is to omit and process all applicable playlists in the source directory)\n"
					+ "isxml - true|false parameter to state whether Rekordbox 5 XML format will be produced or not (default is "+STR_DEFAULT_ISXML.toLowerCase()+")\n"
					+ "isjson - true|false parameter to state whether Tribe XR JSON format will be produced or not (default is "+STR_DEFAULT_ISJSON.toLowerCase()+")\n");
			System.exit(-1);
		}
	
		PlaylistGenerator app=new PlaylistGenerator();
		app.process(cmdSrcDir, cmdDestDir, cmdPlaylist, cmdIsXML, cmdIsJSON);
	}

	/**
	 * This method is used to transform playlists.
	 * @param srcDir the source directory to read playlists
	 * @param destDir the destination directory to write playlists
	 * @param playlist if <code>null</code>, this parameter indicates all applicable playlists should be processed, otherwiwse if not <code>null</code> it specifies the single playlist (without filename extension) to be processed
	 * @param isXML if set to <code>true</code> generates applicable playlists in Rekordbox 5 XML format
	 * @param isJSON if set to <code>true</code> generates applicable playlists in Tribe XR JSON format
	 */
	public void process(String srcDir, String destDir, String playlist, boolean isXML, boolean isJSON)
	{
		ArrayList<String> m3u8Candidates=new ArrayList<String>();
		ArrayList<String> txtCandidates=new ArrayList<String>();

		if(playlist==null || playlist.trim().isEmpty())
		{
			
			File srcDirFile=new File(srcDir);
			File[] files=srcDirFile.listFiles();
			for(File file : files)
			{
				String filename=file.getName();
				if(file.getName().endsWith(STR_M3U8_FILE_EXTENSION))
				{
					m3u8Candidates.add(filename.substring(0, filename.length()-STR_M3U8_FILE_EXTENSION.length()));
				}
				else if(file.getName().endsWith(STR_TXT_FILE_EXTENSION))
				{
					txtCandidates.add(filename.substring(0, filename.length()-STR_TXT_FILE_EXTENSION.length()));				
				}
			}
		}
		else
		{
			m3u8Candidates.add(playlist);
		}
		
		//Only process where we have both m3u8 files and txt files
		for(String m3u8 : m3u8Candidates)
		{
			if(isJSON)
			{
				generateJSON(srcDir,destDir,m3u8);
			}
			
			if(isXML)
			{
				if(!txtCandidates.contains(m3u8))
				{
					continue;
				}
				generateXML(srcDir,destDir,m3u8);
			}
		}
	}
	
	/**
	 * This method is used to generate an XML playlist in the format that was used by Rekordbox 5.
	 * @param directory the directory that holds the m3u8 and txt files.
	 * @param filenamePrefix the name of the file (without the file extension) to convert.
	 */
	public void generateXML(String srcDir, String destDir, String filenamePrefix)
	{
		try {
			//Create XML structure
			Document doc=createDocument(srcDir,destDir,filenamePrefix);
			
			//Dump out to XML file
			writeDocument(doc, destDir, filenamePrefix);
		} catch (Exception e) 
		{
			System.err.println("Exception happened, unable to construct XML playlist : "+e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		System.out.println("Created "+destDir+File.separator+filenamePrefix+STR_XML_FILE_EXTENSION);
	}
	
	/**
	 * This method is used to generate a JSON playlist in the format that was used by Tribe XR.
	 * @param directory the directory that holds the m3u8 and txt files.
	 * @param filenamePrefix the name of the file (without the file extension) to convert.
	 */
	public void generateJSON(String srcDir, String destDir, String filenamePrefix)
	{
		try {
			String m3u8Filename=srcDir+File.separator+filenamePrefix+STR_M3U8_FILE_EXTENSION;
			String jsonFilename=destDir+File.separator+filenamePrefix+STR_JSON_FILE_EXTENSION;

			//Parse the m3u8
			ArrayList<PlaylistEntry> entries=parsem3u8(m3u8Filename);
			
			//Create JSON file
			FileWriter fw=new FileWriter(jsonFilename);
			fw.write("{\n");
			fw.write("\"name\": \""+filenamePrefix+" \",\n");
			fw.write("\"tracks\": [\n");
			
			//Iterate through the tracks
			Iterator<PlaylistEntry> it=entries.iterator();
			PlaylistEntry entry=it.next();
			fw.write("{\n\"trackName\": \""+URLDecoder.decode(entry.title, StandardCharsets.UTF_8.toString())+"\",\n");
			fw.write("\"filename\": \""+URLDecoder.decode(entry.fileLocation.substring(entry.fileLocation.lastIndexOf("/")+1), StandardCharsets.UTF_8.toString())+"\",\n");
			fw.write("\"fullPath\": \""+URLDecoder.decode(entry.fileLocation.substring(6), StandardCharsets.UTF_8.toString())+"\"\n}\n");
			
			while(it.hasNext())
			{
				entry=it.next();
				fw.write(",\n{\n\"trackName\": \""+URLDecoder.decode(entry.title, StandardCharsets.UTF_8.toString())+"\",\n");
				fw.write("\"filename\": \""+URLDecoder.decode(entry.fileLocation.substring(entry.fileLocation.lastIndexOf("/")+1), StandardCharsets.UTF_8.toString())+"\",\n");
				fw.write("\"fullPath\": \""+URLDecoder.decode(entry.fileLocation.substring(6), StandardCharsets.UTF_8.toString())+"\"\n}\n");
			}
			
			
			
			
			//Wrap it up
			fw.write("]\n}");
			fw.close();
		} catch (Exception e) 
		{
			System.err.println("Exception happened, unable to construct JSON playlist : "+e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		System.out.println("Created "+destDir+File.separator+filenamePrefix+STR_JSON_FILE_EXTENSION);
	}
	
	/**
	 * This method is used to create the XML document, root node and preamble before the collection is created.
	 * @param directory the directory that holds the m3u8 and txt files.
	 * @param filenamePrefix the name of the file (without the file extension) to convert.
	 * @return <code>org.w3c.dom.Document</code> instance that contains the basic document structure ready to be populated with a collection.
	 * @throws ParserConfigurationException
	 * @throws IOException 
	 */
	public Document createDocument(String srcDir, String destDir, String filenamePrefix) throws ParserConfigurationException, IOException
	{
		String m3u8Filename=srcDir+File.separator+filenamePrefix+STR_M3U8_FILE_EXTENSION;
		String txtFilename=destDir+File.separator+filenamePrefix+STR_TXT_FILE_EXTENSION;

		
		//Parse the m3u8
		ArrayList<PlaylistEntry> entries=parsem3u8(m3u8Filename);
		
		//Add in the txt data
		if(entries!=null && entries.size()>0)
		{
			parseKTxt(txtFilename,entries);
		}
		else
		{
			return null;
		}
		
		//Create the document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
	    Document retVal = builder.newDocument();
	     
	    //Create the root element
	    Element root=retVal.createElement(STR_DJ_LIST);
	    root.setAttribute(STR_VERSION, STR_VERSION_NUM);
	    retVal.appendChild(root);
	    
	    //Add in the PRODUCT element
	    Element productElement=retVal.createElement(STR_PRODUCT);
	    productElement.setAttribute(STR_NAME, STR_PRODUCT_NAME);
	    productElement.setAttribute(STR_VERSION, STR_PRODUCT_VERSION);
	    productElement.setAttribute(STR_COMPANY, STR_PRODUCT_COMPANY);
	    root.appendChild(productElement);
	    
	    //Iterate through the entries and add the appropriate XML nodes
	    //with the COLLECTION and PLAYLISTS
	    Element collectionElement=retVal.createElement(STR_COLLECTION);
	    collectionElement.setAttribute(STR_ENTRIES, Integer.toString(entries.size()));
	    root.appendChild(collectionElement);
	    
	    Element playlistsElement=retVal.createElement(STR_PLAYLISTS);
	    root.appendChild(playlistsElement);
	    
	    Element parentNodeElement=retVal.createElement(STR_NODE);
	    parentNodeElement.setAttribute(STR_TYPE, "0");
	    parentNodeElement.setAttribute(STR_NAME, "ROOT");
	    parentNodeElement.setAttribute(STR_COUNT, "1");
	    playlistsElement.appendChild(parentNodeElement);
	    
	    Element childNodeElement=retVal.createElement(STR_NODE);
	    childNodeElement.setAttribute(STR_NAME, filenamePrefix);
	    childNodeElement.setAttribute(STR_TYPE, "1");
	    childNodeElement.setAttribute(STR_KEYTYPE, "0");
	    childNodeElement.setAttribute(STR_ENTRIES, Integer.toString(entries.size()));
	    parentNodeElement.appendChild(childNodeElement);
	    
	    
	    //Go through each entry and create track and tempo elements
	    for(PlaylistEntry entry : entries)
	    {
	    	Element trackElement=retVal.createElement(STR_TRACK);
	    	trackElement.setAttribute(STR_TRACKID, entry.trackNumber);
	    	trackElement.setAttribute(STR_NAME,entry.title);
	    	trackElement.setAttribute(STR_ARTIST, entry.artist);
	    	trackElement.setAttribute(STR_COMPOSER, "");
	    	trackElement.setAttribute(STR_ALBUM,"");
	    	trackElement.setAttribute(STR_GROUPING, "");
	    	trackElement.setAttribute(STR_GENRE, entry.genre);
	    	String extension=entry.fileLocation.substring(entry.fileLocation.length()-4).toLowerCase();
	    	
	    	if(extension.equalsIgnoreCase(STR_MP3_FILE_EXTENSION))
	    	{
	    		trackElement.setAttribute(STR_KIND, STR_KIND_MP3);
	    	}
	    	else if(extension.equalsIgnoreCase(STR_WAV_FILE_EXTENSION))
	    	{
	    		trackElement.setAttribute(STR_KIND, STR_KIND_WAV);
	    	}
	    	
	    	trackElement.setAttribute(STR_SIZE, Long.toString(entry.fileSize));
	    	trackElement.setAttribute(STR_TOTALTIME, entry.durationInS);
	    	trackElement.setAttribute(STR_DISCNUMBER, STR_DEFAULT_DISCNUMBER);
	    	trackElement.setAttribute(STR_TRACKNUMBER, STR_DEFAULT_TRACKNUMBER);
	    	trackElement.setAttribute(STR_YEAR, entry.dateAdded.substring(0, 4));
	    	trackElement.setAttribute(STR_AVERAGEBPM, entry.bpm);
	    	trackElement.setAttribute(STR_DATEADDED, entry.dateAdded);
	    	trackElement.setAttribute(STR_BITRATE, STR_DEFAULT_BITRATE);
	    	trackElement.setAttribute(STR_COMMENTS, "");
	    	trackElement.setAttribute(STR_PLAYCOUNT,"0");
	    	trackElement.setAttribute(STR_RATING, "0");
	    	trackElement.setAttribute(STR_LOCATION, entry.fileLocation);
	    	trackElement.setAttribute(STR_REMIX, "");
	    	trackElement.setAttribute(STR_TONALITY, entry.key);
	    	trackElement.setAttribute(STR_LABEL, "");
	    	trackElement.setAttribute(STR_MIX, "");
	    	
	    	collectionElement.appendChild(trackElement);
	    	
	    	//Create the playlist Track entry
	    	Element playlistTrackElement=retVal.createElement(STR_TRACK);
	    	playlistTrackElement.setAttribute(STR_KEY, entry.trackNumber);
	    	childNodeElement.appendChild(playlistTrackElement);
	    }
	    
	     
	    return retVal;
	}
	
	/**
	 * This method is used to parse the m3u8 file exported from Rekordbox.
	 * @param filename the location of the m3u8 file.
	 * @return <code>java.util.ArrayList&lt;KEntry&gt; containing the initial parsed data.
	 * @throws IOException 
	 */
	public ArrayList<PlaylistEntry> parsem3u8(String filename) throws IOException
	{
		ArrayList<PlaylistEntry> retVal=new ArrayList<PlaylistEntry>();
		
		BufferedReader reader=new BufferedReader(new FileReader(filename));
		String line=reader.readLine();
		while(line!=null)
		{
			if(line.startsWith(STR_EXTINF))
			{
				int firstCommaIndex=line.indexOf(",");
				int separatorIndex=line.indexOf(STR_M3U8_SEPARATOR, firstCommaIndex);
				String durationInS=line.substring(STR_EXTINF.length(),firstCommaIndex);
				String artist=line.substring(firstCommaIndex+1,separatorIndex);
				String title=line.substring(separatorIndex+STR_M3U8_SEPARATOR.length());
				File file=new File(reader.readLine());
				String assetFilename=file.toURI().toString();
				PlaylistEntry entry=new PlaylistEntry(durationInS,artist,title,assetFilename);
				retVal.add(entry);
			}
			line=reader.readLine();
		}
		
		reader.close();
		
		return retVal;
	}
	
	/**
	 * This method is used to parse the KUVO txt file exported from Rekordbox.
	 * @param filename the location of the KUVO txt file.
	 * @param The collection containing the initial parsed data from the m3u8 file.
	 * @throws IOException 
	 */
	public void parseKTxt(String filename, ArrayList<PlaylistEntry> entries) throws IOException
	{
		BufferedReader reader=new BufferedReader(new FileReader(filename));
		String line=reader.readLine();
		Iterator<PlaylistEntry> it=entries.iterator();
		
		while(it.hasNext())
		{
			//Ignore the header
			line=reader.readLine();
			if(line.trim().isEmpty())
			{
				continue;
			}
			String[] fields=line.split("\t");			
			PlaylistEntry entry=it.next();
			
			entry.trackNumber=fields[INDEX_TRACKNUM];
			entry.album=fields[INDEX_ALBUM];
			entry.genre=fields[INDEX_GENRE];
			entry.bpm=fields[INDEX_BPM];
			entry.key=fields[INDEX_KEY];
			entry.dateAdded=fields[INDEX_DATEADDED];
		}
		
		reader.close();
	}
	
	/**
	 * This method is used to pretty print an XML playlist in the format used by Rekordbox 5 to a file.
	 * @param doc the XML <code>Document</code> instance to be output.
	 * @param directory the directory where the file should be written to.
	 * @param filenamePrefix the name of the file to write to (without the <code>.xml</code> extension.
	 * @throws IOException
	 * @throws TransformerException
	 */
	public void writeDocument(Document doc, String directory, String filenamePrefix) throws IOException, TransformerException
	{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource dom = new DOMSource(doc);
		FileWriter writer = new FileWriter(new File(directory+File.separator+filenamePrefix+STR_XML_FILE_EXTENSION));
		StreamResult result = new StreamResult(writer);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(dom, result);
		writer.close();
	}
	
	/**
	 * This class is used to hold the playlist data parsed from Rekordbox.
	 * 
	 * @author Les Carter
	 */
	public class PlaylistEntry
	{
		public String durationInS="";
		public String artist="";
		public String title="";
		public String fileLocation="";
		public long fileSize=0;
		public String trackNumber="";
		public String album="";
		public String genre="";
		public String bpm="";;
		public String key="";
		public String dateAdded="";
		
		public PlaylistEntry(String durationInS, String artist, String title, String fileLocation)
		{
			super();
			this.durationInS=durationInS;
			this.artist=artist;
			this.title=title;
			this.fileLocation=fileLocation;
			
			fileSize=new File(fileLocation).length();
		}	
	}
	
	
}
