// Main logic layer for the AIBot

import java.util.LinkedList;
import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import struct.FrameData;
import struct.GameData;
import struct.MotionData;

public class LogicLayer
{
	ProcessingLayer PL;
	GameVariables GV;
	
	Fighter Bot;
	Fighter Enemy;

	int distance;

	FrameData simulatedFrameData;

	double bestScore;
	Action bestAction;

	LinkedList<Action> generatedActions = new LinkedList<Action>();

	public LogicLayer(GameData arg0, boolean arg1)
	{
		GV = new GameVariables();
		GV.FrameData = new FrameData();
		GV.CommandCenter = new CommandCenter();
		GV.GameData = arg0;
		GV.Simulator = GV.GameData.getSimulator();
		
		Bot = new Fighter();
		Enemy = new Fighter();
		
		Bot.Player = arg1;
		Enemy.Player = !arg1;
		
		Bot.Motion = GV.GameData.getMotionData(Bot.Player);
		Enemy.Motion = GV.GameData.getMotionData(Enemy.Player);

		Bot.AvaliableActions = new LinkedList<Action>();
		Enemy.AvaliableActions = new LinkedList<Action>();
	}

	// Required for game
	public void close()
	{

	}

	void setBaseActions()
	{
		if (Bot.Data.getState() != State.AIR)
		{
			Bot.AllActions = Actions.ZenGroundActions;

		} else
		{
			Bot.AllActions = Actions.ZenAirActions;
		}
	}

	void generateActions()
	{
		bestScore  = -9999;
		bestAction  = null;
		generatedActions.clear();
		for (Action action : Bot.AllActions)
		{
			double hpScore = PL.GetHpScore(action);
			MotionData motion = Bot.Motion.get(action.ordinal());

			if (hpScore > 0)
			{
				generatedActions.add(action);
				double turnscore = (double) hpScore + (motion.attackDownProp ? 30.0 : 0.0)
						- ((double) motion.attackStartUp) * 0.01 - ((double) motion.cancelAbleFrame) * 0.0001;
				if (turnscore > bestScore )
				{
					bestScore  = turnscore;
					bestAction  = action;
				}
			}
		}
	}

	public void getInformation(FrameData frameData)
	{
		this.GV.FrameData = frameData;

		if (frameData.getFramesNumber() >= 0)
		{
			if (frameData.getFramesNumber() < 14)
			{
				simulatedFrameData = new FrameData(frameData);
			}
			else
			{
				simulatedFrameData = GV.Simulator.simulate(frameData, Bot.Player, null, null, 14);
			}

			GV.CommandCenter.setFrameData(simulatedFrameData, Bot.Player);
			distance = simulatedFrameData.getDistanceX();
			Bot.Data = simulatedFrameData.getCharacter(Bot.Player);
			Enemy.Data = simulatedFrameData.getCharacter(Enemy.Player);

			PL = new ProcessingLayer(simulatedFrameData, GV.GameData, Bot.Player, ProcessingLayer.NONACT);

			setBaseActions();

			if (Enemy.Data.getState() != State.AIR)
			{
				Enemy.AllActions = Actions.GroundActions;
			}
			else
			{
				Enemy.AllActions = Actions.AirActions;
			}
			
			Bot.AvaliableActions = PL.GetAvailableActions(true, Bot.AllActions);
			Enemy.AvaliableActions = PL.GetAvailableActions(false, Enemy.AllActions);

			generateActions();
		}
	}

	public String DoSomething()
	{
		String result = "";
		
		if (ImDown())
		{
			result = "1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1";
		}

		Action resultAction = null;

		if (Bot.Data.getState() != State.AIR)
		{
			if (100 < distance && distance < 300)
			{
				if (PL.HitboxChecker(resultAction = Action.STAND_D_DF_FC))
				{
					result = resultAction.name();
				}
			}
		}
		if (bestAction != null)
		{
			if (Bot.Data.getState() != State.AIR)
			{
				LinkedList<Action> avaliableActions = PL.GetAvailableActions(false, Action.NEUTRAL, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB);

				LinkedList<Action> neutralActions = PL.GetAvailableActions(false, Action.NEUTRAL);

				if (PL.GetMinHpScore(resultAction = Action.STAND_D_DF_FC, neutralActions) > 0)
				{
					result = resultAction.name();
				}
				else if (PL.GetMinHpScore(resultAction = Action.STAND_B, avaliableActions) > 0)
				{
					result = resultAction.name();
				}
				else if (PL.GetMinHpScore(resultAction = Action.CROUCH_FB, avaliableActions) > 0)
				{
					result = resultAction.name();
				}
				else if (PL.GetMinHpScore(resultAction = Action.STAND_D_DB_BB, avaliableActions) > 0)
				{
					result = resultAction.name();
				}
				else if (PL.GetMinHpScore(resultAction = Action.STAND_F_D_DFA, avaliableActions) > 0)
				{
					result = resultAction.name();
				}
				else
				{
					result = (bestAction).name();
				}
			}
			else
			{
				result = (bestAction).name();
			}
		}
		else
		{
			if (Bot.Data.getState() != State.AIR)
			{
				if (Bot.Data.getHp() - Enemy.Data.getHp() > 100)
				{
					LinkedList<Action> avaliableActions = PL.GetAvailableActions(true, Action.BACK_STEP, Action.JUMP, Action.FOR_JUMP, Action.FORWARD_WALK, Action.BACK_JUMP);

					result = (PL.GetBestActionWhenSkillUsed(avaliableActions)).name();
				}
				else
				{
					if (distance < 300)
					{
						LinkedList<Action> avaliableActions = PL.GetAvailableActions(true, Action.FOR_JUMP, Action.FORWARD_WALK, Action.JUMP, Action.BACK_JUMP, Action.BACK_STEP);
						result = (PL.GetBestActionWhenSkillUsed(avaliableActions)).name();
					}
					else
					{
						LinkedList<Action> avaliableActions = PL.GetAvailableActions(true, Action.FORWARD_WALK, Action.FOR_JUMP, Action.JUMP, Action.BACK_JUMP, Action.BACK_STEP);
						result = (PL.GetBestActionWhenSkillUsed(avaliableActions)).name();
					}
				}
			}
			else
			{
				result = (Action.AIR_GUARD).name();
			}
		}
		
		return result;
	}
	
	public Boolean ImDown()
	{
		Action action = simulatedFrameData.getCharacter(Bot.Player).getAction();

		return ((action == Action.DOWN || action == Action.RISE || action == Action.CHANGE_DOWN) && simulatedFrameData.getCharacter(Enemy.Player).getState() != State.AIR);
	}
}
