import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import enumerate.Action;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.HitArea;
import struct.MotionData;

public class ProcessingLayer
{
	public final static int SIMULATION_LIMIT = 60;
	public final static Action NONACT = Action.NEUTRAL;
	
	public FrameData nonActionFrame;
	FrameData motionFrame;
	
	HashMap<List, FrameData> map;
	
	GameData gd;
	Simulator simulator;
	boolean player;
	
	private ArrayList<MotionData> botMotion;
	private ArrayList<MotionData> enemyMotion;
	
	LinkedList<Action> AirSkillActions;
	LinkedList<Action> GroundSkillActions;

	public ProcessingLayer(FrameData motionFrame, GameData gd, boolean player, Action preAct)
	{
		this.motionFrame = motionFrame;
		this.gd = gd;
		simulator = gd.getSimulator();
		this.player = player;
		botMotion = gd.getMotionData(this.player);
		enemyMotion = gd.getMotionData(!this.player);
		
		AirSkillActions = new LinkedList<Action>();
		GroundSkillActions = new LinkedList<Action>();
		
		for (Action ac : Actions.AirActions)
		{
			MotionData motion = botMotion.get(ac.ordinal());
			
			if (motion.getAttackSpeedX() != 0 || motion.getAttackSpeedY() != 0)
			{
				AirSkillActions.add(ac);
			}
		}
		
		for (Action ac : Actions.GroundActions)
		{
			MotionData motion = botMotion.get(ac.ordinal());
			
			if (motion.getAttackSpeedX() != 0 || motion.getAttackSpeedY() != 0)
			{
				GroundSkillActions.add(ac);
			}
		}

		map = new HashMap<List, FrameData>();
		
		nonActionFrame = GetFrame(preAct, NONACT);
	}

	public boolean CanPlayerDoThisAction(Action act, boolean player)
	{
		ArrayList<MotionData> motions = (player ? botMotion : enemyMotion);
		CharacterData ch = (player ? motionFrame.getCharacter(this.player) : motionFrame.getCharacter(!this.player));
		return (motions.get(act.ordinal()).getAttackStartAddEnergy() + ch.getEnergy() >= 0);

	}

	public LinkedList<Action> GetAvailableActions(boolean player, Action... actions)
	{
		LinkedList<Action> avaliableActions = new LinkedList<Action>();
		
		for (Action ac : actions)
		{
			if (CanPlayerDoThisAction(ac, player))
			{
				avaliableActions.add(ac);
			}
		}

		return avaliableActions;
	}

	public FrameData GetFrame(Action botAction, Action enemyAction)
	{
		Action nextBotAction, nextEnemyAction;
		
		if (CanPlayerDoThisAction(botAction, true))
		{
			nextBotAction = botAction;
		}
		else
		{
			nextBotAction = NONACT;
		}
		
		if (CanPlayerDoThisAction(enemyAction, false))
		{
			nextEnemyAction = enemyAction;
		}
		else
		{
			nextEnemyAction = NONACT;
		}
		
		List<Action> key = new ArrayList<Action>();
		
		key.add(nextBotAction);
		key.add(nextEnemyAction);

		if (!map.containsKey(key))
		{
			FrameData value = this.simulator.simulate(motionFrame, player,
					new LinkedList<Action>(Arrays.asList(nextBotAction)),
					new LinkedList<Action>(Arrays.asList(nextEnemyAction)), SIMULATION_LIMIT);
			map.put(key, value);
		}

		return map.get(key);
	}

	public FrameData GetActionFrame(Action action)
	{
		return GetFrame(action, NONACT);
	}

	public double GetHpScore(Action action)
	{
		return GetHpScore(action, NONACT);
	}

	public double GetHpScore(Action botAction, Action enemyAction)
	{
		FrameData fd = GetFrame(botAction, enemyAction);
		
		double botHpDifference = fd.getCharacter(player).getHp() - nonActionFrame.getCharacter(player).getHp();
		double enemyHpDifference = fd.getCharacter(!player).getHp() - nonActionFrame.getCharacter(!player).getHp();

		return botHpDifference - enemyHpDifference;
	}

	public double GetMinHpScoreWhenSkillUsed(Action botAction)
	{
		double min = Double.MAX_VALUE;
		
		for (Action enemyAction : GroundSkillActions)
		{
			double score = GetHpScore(botAction, enemyAction);
			
			if (score < min)
			{
				min = score;
			}
		}
		return min;
	}

	public Action GetBestActionWhenSkillUsed(List<Action> botActions)
	{
		double max = Double.MIN_VALUE;
		Action bestAction = Action.FORWARD_WALK;
		for (Action botAction : botActions)
		{
			double score = GetMinHpScoreWhenSkillUsed(botAction);
			if (score > max)
			{
				max = score;
				bestAction = botAction;
			}
		}
		return bestAction;
	}

	public double GetMinHpScore(Action botAction, List<Action> enemyActions)
	{
		double min = Double.MAX_VALUE;

		for (Action enemyAction : enemyActions)
		{
			double score = GetHpScore(botAction, enemyAction);
			if (score < min)
			{
				min = score;
			}
		}

		return min;
	}

	public Action GetBestAction(List<Action> botActions, List<Action> enemyActions)
	{

		double alpha = Double.MIN_VALUE;
		Action bestAction = Action.FORWARD_WALK;
		for (Action botAction : botActions)
		{
			double min = Double.MAX_VALUE;

			for (Action enemyAction : enemyActions)
			{
				double score = GetHpScore(botAction, enemyAction);
				if (score < min)
				{
					min = score;
					if (min < alpha)
						break;
				}
			}
			
			if (min > alpha)
			{
				alpha = min;
				bestAction = botAction;
			}
		}
		return bestAction;
	}

	public boolean HitboxChecker(Action action)
	{
		Coordinate botCoordinates = new Coordinate();
		boolean result = false;
		
		CharacterData bot = motionFrame.getCharacter(player);
		CharacterData enemy = motionFrame.getCharacter(!player);
		
		if (this.CanPlayerDoThisAction(action, true))
		{
			result = false;
		}
		else
		{
			MotionData motion = botMotion.get(action.ordinal());
			HitArea hitbox = motion.attackHitArea;

			// Get initial values for top and bottom.
			botCoordinates.Top = bot.getY() + hitbox.getTop();
			botCoordinates.Bottom = bot.getY() + hitbox.getBottom();
			
			botCoordinates.Bottom += motion.getAttackStartUp() * motion.getSpeedY();
			botCoordinates.Top += motion.getAttackStartUp() * motion.getSpeedY();
			
			// Add velocity from attack movement.
			if (motion.getAttackSpeedY() > 0)
			{
				botCoordinates.Bottom += motion.attackActive * motion.getAttackSpeedY();
			}
			else
			{
				botCoordinates.Top += motion.attackActive * motion.getAttackSpeedY();
			}
			
			if (motion.getSpeedY() > 0)
			{
				botCoordinates.Bottom += motion.attackActive * motion.getSpeedY();
			}
			else
			{
				botCoordinates.Top += motion.attackActive * motion.getSpeedY();
			}

			int orientation = 1;

			if (bot.isFront())
			{
				botCoordinates.Left = bot.getX() + hitbox.getLeft();
				botCoordinates.Right = bot.getX() + hitbox.getRight();
			}
			else
			{
				orientation = -1;
				botCoordinates.Left = bot.getX() + bot.getGraphicSizeX() - hitbox.getRight();
				botCoordinates.Right = bot.getX() + bot.getGraphicSizeX() - hitbox.getLeft();
			}

			botCoordinates.Left += motion.getAttackStartUp() * motion.getSpeedX() * orientation;
			botCoordinates.Right += motion.getAttackStartUp() * motion.getSpeedX() * orientation;

			if (motion.getAttackSpeedX() * orientation > 0)
			{
				botCoordinates.Right += motion.attackActive * motion.getAttackSpeedX() * orientation;
			}
			else
			{
				botCoordinates.Left += motion.attackActive * motion.getAttackSpeedX() * orientation;
			}
			
			if (motion.getSpeedX() * orientation > 0)
			{
				botCoordinates.Right += motion.attackActive * motion.getSpeedX() * orientation;
			}
			else
			{
				botCoordinates.Left += motion.attackActive * motion.getSpeedX() * orientation;
			}			

			Coordinate enemyCoordinates = new Coordinate(enemy.getTop(), enemy.getBottom(), enemy.getLeft(), enemy.getRight());
			

			if (botCoordinates.Right < enemyCoordinates.Left)
			{
				result = false;
			}
			else if (enemyCoordinates.Right < botCoordinates.Left)
			{
				result = false;
			}
			else if (botCoordinates.Bottom < enemyCoordinates.Top)
			{
				result = false;
			}
			else if (enemyCoordinates.Bottom < botCoordinates.Top)
			{
				result = false;
			}
		}
		
		return result;
	}

}
