package com.checkers.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.List;

/** Immutable sequence of board snapshots reconstructed from persisted moves. */
public class GameReplay {
    private final List<GameLogic> frames=new ArrayList<>();
    private int position=0; private boolean playing=false;

    /** Compatibility constructor: replays the default (American) ruleset. */
    public GameReplay(ArrayNode savedMoves){this(savedMoves,new GameLogic());}

    /** Variant-aware constructor. Pass a fresh GameLogic created by GameFactory. */
    public GameReplay(ArrayNode savedMoves,GameLogic initialGame){
        if(savedMoves==null)throw new IllegalArgumentException("Saved moves are required.");
        if(initialGame==null)throw new IllegalArgumentException("Initial game state is required.");
        GameLogic game=initialGame.copy();frames.add(game.copy());int expected=1;
        for(JsonNode saved:savedMoves){
            JsonNode n=saved.path("move_number"),data=saved.path("move_data");
            if(!n.isIntegralNumber()||!n.canConvertToInt()||n.asInt()!=expected)
                throw new IllegalArgumentException("Saved moves must be numbered consecutively.");
            JsonNode sr=data.path("start"),er=data.path("end");
            JsonNode fr=data.path("fromRow"),fc=data.path("fromCol"),tr=data.path("toRow"),tc=data.path("toCol");
            boolean coords=fr.isIntegralNumber()&&fc.isIntegralNumber()&&tr.isIntegralNumber()&&tc.isIntegralNumber();
            if(!coords&&(!sr.isIntegralNumber()||!sr.canConvertToInt()||!er.isIntegralNumber()||!er.canConvertToInt()))
                throw new IllegalArgumentException("Saved move "+expected+" has no valid coordinates.");
            int start=coords?game.getBoard().indexAt(fr.asInt(),fc.asInt()):sr.asInt();
            int end=coords?game.getBoard().indexAt(tr.asInt(),tc.asInt()):er.asInt();
            if(start<0||end<0||!game.move(start,end))throw new IllegalArgumentException("Saved move "+expected+" cannot be replayed.");
            frames.add(game.copy());expected++;
        }
    }
    public synchronized GameLogic getCurrentGame(){return frames.get(position).copy();}
    public synchronized int getPosition(){return position;}
    public int getMoveCount(){return frames.size()-1;}
    public synchronized boolean hasNext(){return position<getMoveCount();}
    public synchronized boolean hasPrevious(){return position>0;}
    public synchronized GameLogic next(){if(hasNext())position++;if(!hasNext())playing=false;return getCurrentGame();}
    public synchronized GameLogic previous(){playing=false;if(hasPrevious())position--;return getCurrentGame();}
    public synchronized GameLogic restart(){playing=false;position=0;return getCurrentGame();}
    public synchronized GameLogic seek(int move){if(move<0||move>getMoveCount())throw new IllegalArgumentException("Replay position is outside the saved game.");playing=false;position=move;return getCurrentGame();}
    public synchronized void play(){playing=hasNext();}
    public synchronized void pause(){playing=false;}
    public synchronized boolean isPlaying(){return playing;}
    public synchronized GameLogic tick(){return playing?next():getCurrentGame();}
}
