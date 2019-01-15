/*
 * AIBot for FightingICE
 * Umut M. Dabager
 * 504161552
 */

// Must have imports for the FightingICE AI Interface
import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import struct.FrameData;
import struct.GameData;
import struct.Key;

public class AIBot implements AIInterface
{
	private boolean player;
	private Key inputKey;
	
	private GameVariables GV;
	private LogicLayer LL;
	
	private boolean isFirstFrame = true;
	
	@Override
	public void close()
	{
		LL.close();
	}

	@Override
	public void getInformation(FrameData arg0)
	{
		GV.FrameData = arg0;
		if (GV.FrameData.getFramesNumber() >= 0)
		{
			if (isFirstFrame)
			{
				isFirstFrame = false;
				initializeLogicLayer();
			}
		}
		if (LL != null)
		{
			LL.getInformation(GV.FrameData);
		}
	}
	
	private void initializeLogicLayer()
	{
		GV.CommandCenter.setFrameData(GV.FrameData, player);
		
		if (GV.GameData.getAiName(!player).equals("MctsAi"))
		{
			GV.GameMode = GameModes.SPEED;
		}
		else
		{
			GV.GameMode = GameModes.NORMAL;
		}

		if (GV.Character == Characters.ZEN && GV.GameMode == GameModes.NORMAL)
		{
			LL = new LogicLayer(GV.GameData, player);
		}
		/*
		 * TO-DO : Other characters and gamemodes
		 */
	}

	@Override
	public int initialize(GameData arg0, boolean arg1)
	{
		inputKey = new Key();
		player = arg1;
		GV = new GameVariables();
		GV.FrameData = new FrameData();
		GV.CommandCenter = new CommandCenter();
		GV.GameData = arg0;
		
		String characterName = GV.GameData.getCharacterName(player);
		
		if (characterName == "ZEN")
		{
			GV.Character = Characters.ZEN;
		}
		else if (characterName == "GARNET")
		{
			GV.Character = Characters.GARNET;
		}
		else GV.Character = Characters.LUD;
		
		return 0;
	}

	@Override
	public Key input()
	{
		// Return the global parameter inputKey as a result.
		// It will be changed in the processing part.
		return inputKey;
	}

	
	long startingTime;
	long maximumTime;
	@Override
	public void processing()
	{
		startingTime = System.nanoTime();
		
		if(!GV.FrameData.getEmptyFlag() && GV.FrameData.getRemainingTime()>0)
		{

			if (GV.CommandCenter.getSkillFlag())
			{
				// If there is a previous "command" still in execution, then keep doing it
				inputKey = GV.CommandCenter.getSkillKey();
			}
			else
			{
				inputKey.empty();
				GV.CommandCenter.setFrameData(GV.FrameData, player);
				GV.CommandCenter.skillCancel();
				GV.CommandCenter.commandCall(LL.DoSomething());
			}
			
			if(GV.FrameData.getFramesNumber() >= 0)
			{
				long stepTime = System.nanoTime() - startingTime;
				if(stepTime > maximumTime)
				{
					maximumTime = stepTime;
				}
				
				if((GV.FrameData.getFramesNumber() % 10) == 0)
				{
					maximumTime = 0;
				}
			}
		}
	}

	@Override
	public void roundEnd(int arg0, int arg1, int arg2)
	{
		// TODO Auto-generated method stub

	}
}
