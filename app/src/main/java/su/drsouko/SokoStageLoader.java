package su.drsouko;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
//import android.util.Log;

public class SokoStageLoader {
	private String stagesFilename;
	private String stagesTitle, stagesCopyright, stageTitle;
	private int roomWidth, roomHeight;
	private int chrx, chry;
	private char[][] room;
	private int targets;
	public static final String STAGE_SEP="--";
	private Context ctx;
	private int maxLoadedStage;
	public SokoStageLoader(Context theContext, String path) {
		ctx=theContext;
		stagesFilename=path;
	}
	public SokoStageLoader(Context theContext) {
		ctx=theContext;
	};
	public void setStagesFilename(String thePath) { stagesFilename=thePath; }
	public String stagesFilename() { return stagesFilename; }
	public String stagesTitle() { return stagesTitle; }
	public String stagesCopyright() { return stagesCopyright; }
	public String stageTitle() { return stageTitle; }
	public int roomWidth() { return roomWidth; }
	public int roomHeight() { return roomHeight; }
	public int chrx() { return chrx; }
	public int chry() { return chry; }
	public char[][] room() { return room; }
	public int maxLoadedStage() { return maxLoadedStage; }
	public int numTargets() { return targets; }
	private InputStream InputStreamFor(String thePath) {
		InputStream stgIn;
		if(thePath==null) {
			stgIn=ctx.getResources().openRawResource(R.raw.stages);
		} else {
			try {
				stgIn=new FileInputStream(thePath);
			} catch (java.io.FileNotFoundException e) {
				stgIn=null;
			}
		}
		return stgIn;
	}
	public boolean loadStage(int stageNum){
		InputStream stgIn=InputStreamFor(stagesFilename);
		if(stgIn==null) { return false; }
		BufferedReader br=null;
		try {
			br=new BufferedReader(new InputStreamReader(stgIn,"utf-8"));
			stagesTitle=br.readLine();
			stagesCopyright=br.readLine();
			if(stageNum==0) { return true; }
			for(int i=0; i<stageNum; ) {
				String line=br.readLine();
				if(line==null) { return false; }
				if(STAGE_SEP.equals(line)) {
					i++;
				} else {
					maxLoadedStage=i;
				}
			}
			stageTitle=br.readLine();
			if(stageTitle==null || stageTitle.length()<1) { return false; }
			LinkedList<String> a=new LinkedList<String>();
			int max_linelen=0;
			while(br.ready()) {
				String line=br.readLine();
				if(line==null) { break; }
				if(line.equals(STAGE_SEP)) { break; }
				a.add(line);
				max_linelen=Math.max(max_linelen, line.length());
			}
			roomHeight=Math.max(1,a.size());
			roomWidth=Math.max(1,max_linelen);
			room=new char[roomHeight][roomWidth];
			int y=0;
			targets=0;
			while(!a.isEmpty()) {
				String line=a.pop();
				int x=0;
				if(line!=null) {
					for(; x<line.length(); x++) {
						char c=line.charAt(x);
						if(c=='@') {
							chrx=x;
							chry=y;
							c='.';
						} else if(c=='+') {
							chrx=x;
							chry=y;
							c='x';
						}
						if(c=='x') { targets++; }
						room[y][x]=c;
					}
				}
				for(; x<roomWidth; x++) {
					room[y][x]=' ';
				}
				y++;
			}
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if(br!=null) {
					br.close();
				} else {
					stgIn.close();
				}
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}
	public String getEntireFile() {
		InputStream stgIn=InputStreamFor(stagesFilename);
		String res="";
		if(stgIn==null) { return null; }
		BufferedReader br=new BufferedReader(new InputStreamReader(stgIn));
		char[] abuf=new char[4096];
		try {
			int nread;
			do {
				nread=br.read(abuf);
				if(nread>0) {
					String bufstr=String.valueOf(abuf);
					if(nread<abuf.length) {
						res=res.concat(bufstr.substring(0,nread));
					} else {
						res=res.concat(bufstr);
					}
				}
			} while (nread>0);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				return null;
			}
		}
		return res;
	}
	public boolean writeEntireFile(String theContents) {
		OutputStream ostream;
		try {
			ostream=new FileOutputStream(stagesFilename);
		} catch (Exception e) {
			return false;
		}
		BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(ostream));
		try {
			bw.write(theContents);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}
	public static final String STAGE_COL="title";
	public List<Map<String,String>> getStageTitles() {
		InputStream stgIn=InputStreamFor(stagesFilename);
		if(stgIn==null) { return null; }
		List<Map<String,String>> result=new LinkedList<Map<String,String>>();
		BufferedReader br=new BufferedReader(new InputStreamReader(stgIn));
		int state=0;
		try {
			while(br.ready()) {
				String line=br.readLine();
				if(state==0) {
					if("--".equals(line)) {
						state=1;
					}
				} else {
					HashMap<String,String> stgHash=new HashMap<String,String>();
					stgHash.put(STAGE_COL, line);
					result.add(stgHash);
					state=0;
				}
			}
		} catch (IOException e) {
			return null;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				return null;
			}
		}
		return result;
	}
	public boolean minimalSanityCheck(String allegedStageData) {
		BufferedReader br=new BufferedReader(new StringReader(allegedStageData));
		try {
			stagesTitle=br.readLine();
			if(stagesTitle==null || stagesTitle.length()<1) {
				//Log.println(Log.DEBUG, "sanityCheck", "stage title empty");
				return false; }
			stagesCopyright=br.readLine();
			if(stagesCopyright==null) {
				//Log.println(Log.DEBUG, "sanityCheck", "copyright empty");
				return false; }
			String sep=br.readLine();
			if(!STAGE_SEP.equals(sep)) { return false; }
			stageTitle=br.readLine();
			if(stageTitle==null || stageTitle.length()<1) { return false; }
		} catch (IOException e) {
			return false;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}
}
