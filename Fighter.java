import java.util.ArrayList;
import java.util.LinkedList;
import enumerate.Action;
import struct.CharacterData;
import struct.MotionData;

public class Fighter
{
	boolean Player;
	CharacterData Data;
	ArrayList<MotionData> Motion;
	Action[] AllActions;
	LinkedList<Action> AvaliableActions;
}
