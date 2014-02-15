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

public class SokoFileWriter {
	private SokoGameState gameState;
	private int writtenStages=0;
	public SokoFileWriter(SokoGameState gameState) {
		this.gameState=gameState;
	}
	private void writeFileHeader(BufferedWriter bw) throws IOException {
		bw.write(gameState.stagesTitle); bw.newLine();
		bw.write(gameState.stagesCopyright); bw.newLine();
	}
	private void skipFileHeader(BufferedReader br) throws IOException {
		br.readLine(); // discard stages title line
		br.readLine(); // discard copyright line
		br.readLine(); // discard stage separator
	}
	private boolean copyOneStage(BufferedReader br, BufferedWriter bw) throws IOException {
		if(bw!=null) {
			bw.write(SokoStageLoader.STAGE_SEP);
			bw.newLine();
			writtenStages++;
		}
		while(br.ready()) {
			String line=br.readLine();
			if(line==null) { return false; }
			else if(SokoStageLoader.STAGE_SEP.equals(line)) {
				return true;
			} else if(bw!=null) {
				bw.write(line);
				bw.newLine();
			}
		}
		return false;
	}
	private void writeCurrentStage(BufferedWriter bw) throws IOException {
		bw.write(SokoStageLoader.STAGE_SEP); bw.newLine();
		writtenStages++;
		bw.write(gameState.stageTitle); bw.newLine();
		int[] xmin=new int[gameState.room.length];
		int[] xmax=new int[gameState.room.length];
		int ymin=gameState.room.length-1;
		int ymax=0;
		for(int y=0; y<gameState.room.length; y++) {
			xmin[y]=gameState.room[y].length-1;
			xmax[y]=0;
			for(int x=0; x<gameState.room[y].length; x++) {
				if(gameState.room[y][x]!=' ') {
					if(ymin>y) { ymin=y; }
					if(ymax<y) { ymax=y; }
					if(xmin[y]>x) { xmin[y]=x; }
					if(xmax[y]<x) { xmax[y]=x; }
				}
			}
		}
		int glo_xmin=gameState.room[0].length;
		for(int xm: xmin) { if(xm<glo_xmin) { glo_xmin=xm; } }
		int chrx=gameState.chrX();
		int chry=gameState.chrY();
		for(int y=ymin; y<=ymax; y++) {
			for(int x=glo_xmin; x<=xmax[y]; x++) {
				char c;
				if(x==chrx && y==chry) {
					c='@';
				} else {
					c=gameState.room[y][x];
				}
				bw.write(c);
			}
			bw.newLine();
		}
	}
	public boolean saveStage(File tmpfiledir) {
		return saveStage(tmpfiledir,false,0,false);
	}
	private boolean saveStage(File tmpfiledir,boolean removeMode, int swapWith, boolean dupMode) {
		OutputStream ostream;
		InputStream istream;
		File tmpFile, inFile;
		try {
			tmpFile=File.createTempFile("sokosave", ".tmp", tmpfiledir);
			ostream=new FileOutputStream(tmpFile);
			inFile=new File(gameState.stagesFilename);
			istream=new FileInputStream(inFile);
		} catch (IOException e1) {
			return false;
		}
		BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(ostream));
		BufferedReader br=new BufferedReader(new InputStreamReader(istream));
		try {
			writeFileHeader(bw);
			skipFileHeader(br);
			int thisStage=1;
			boolean cont=true;
			int target_stg;
			if(0<swapWith && swapWith<=gameState.stage) {
				target_stg=gameState.stage+1;
			} else {
				target_stg=gameState.stage;
			}
			do {
				if(swapWith==thisStage) {
					writeCurrentStage(bw);
				} else if(thisStage==target_stg) {
					cont=copyOneStage(br,null); // skip
					if(!removeMode) { writeCurrentStage(bw); }
					if(dupMode) {writeCurrentStage(bw); }
				} else {
					cont=copyOneStage(br,bw);
				}
				thisStage++;
			} while(cont);
		} catch (IOException e) {
		} finally {
			boolean res=true;
			try {
				bw.close();
			} catch (IOException e) { res=false; }
			try {
				br.close();
			} catch (IOException e) { res=false; }
			if(!res) {return res; }
		}
		tmpFile.renameTo(inFile);
		return true;
	}
	public boolean appendOneStage(String template) {
		OutputStream ostream;
		try {
			ostream=new FileOutputStream(gameState.stagesFilename,true);
		} catch (FileNotFoundException e) {
			return false;
		}
		BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(ostream));
		try {
			bw.write(template);
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
	public boolean removeCurrentStage(File tmpDir, String templateIfEmpty) {
		writtenStages=0;
		boolean success=saveStage(tmpDir,true,0,false);
		if(success && writtenStages==0) {
			success &= appendOneStage(templateIfEmpty);
		}
		return success;
	}
	public boolean moveCurrentStageTo(File tmpDir, int beforeThisStage) {
		return saveStage(tmpDir,true,beforeThisStage,false);
	}
	public boolean dupCurrentStage(File tmpDir) {
		return saveStage(tmpDir,false,0,true);
	}
}
