package su.drsouko;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.content.Context;
//import android.util.Log;
import android.util.SparseIntArray;

public class HighscoreMgr {
	//private Context context;
	private String hsFilePath;
	private boolean dirty=false;
	private static final String scoreSuffix="scr";
	private SparseIntArray scores; 
	public HighscoreMgr(Context theContext, String ofFile) {
		//context=theContext;
		hsFilePath=scoreFilePath(theContext,ofFile);
		
	}
	public void load() {
		scores=new SparseIntArray();
		InputStream instream;
		dirty=false;
		try {
			instream=new FileInputStream(hsFilePath);
		} catch (FileNotFoundException e) {
			return;
		}
		BufferedReader br=new BufferedReader(new InputStreamReader(instream));
		int i=1;
		try {
			while(br.ready()) {
				String line=br.readLine();
				if(line==null) {
					break;
				}
				try {
					int score=Integer.parseInt(line);
					scores.put(i, score);
				} catch (NumberFormatException e) {
					;
				}
				i++;
			}
		} catch (IOException e) {
			return;
		} finally {
			try {
				br.close();
			} catch(IOException e) {}
		}
	}
	public boolean save() {
		if(dirty) {
			dirty=false;
			int maxStage=0;
			int hashSz=scores.size();
			for(int i=0; i<hashSz; i++) {
				maxStage=Math.max(maxStage, scores.keyAt(i));
			}
			OutputStream ostream;
			try {
				ostream=new FileOutputStream(hsFilePath);
			} catch (Exception e) {
				return false;
			}
			BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(ostream));
			try {
				for(int i=1; i<=maxStage; i++) {
					bw.write(Integer.toString(scoreOfStage(i))+"\n");
				}
			} catch (IOException e) {
				return false;
			} finally {
				try {
					bw.close();
				} catch(IOException e) {}
			}
		}
		return true;
	}
	public int scoreOfStage(int theStage) {
		return scores.get(theStage);
	}
	public boolean putScore(int ofStage, int theScore) {
		int prevScore=scoreOfStage(ofStage);
		boolean isHighscore=(prevScore==0) || (prevScore>theScore);
		if(isHighscore) {
			scores.put(ofStage, theScore);
			dirty=true;
		}
		return isHighscore;
	}
	public static String scoreFilePath(Context ctx, String ofFile) {
		String thePath;
		if(ofFile==null) {
			String filesPath=ctx.getFilesDir().getPath();
			thePath=filesPath+File.separator+"00000000."+scoreSuffix;
		} else {
			if(ofFile.charAt(ofFile.length()-4)=='.') {
				thePath=ofFile.substring(0, ofFile.length()-3)+scoreSuffix;
			} else {
				thePath=ofFile+"."+scoreSuffix;
			}
		}
		//Log.println(Log.DEBUG, "JAVA", "HighscoreFile="+thePath);
		return thePath;
	}
	public int minUnclearedStage() {
		int max=scores.size();
		for(int i=1; i<=max; i++) {
			if(scores.get(i)==0) {
				return i;
			}
		}
		return max+1;
	}
}
