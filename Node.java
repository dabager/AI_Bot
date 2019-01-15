import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.CommandCenter;
import enumerate.Action;
import simulator.Simulator;
import struct.FrameData;
import struct.GameData;

public class Node
{
	public long UCT_TIME;
	public static final double UCB_C = 3;
	public static final int UCT_TREE_DEPTH = 2;
	public static final int UCT_CREATE_NODE_THRESHOLD = 10;
	public static final int SIMULATION_TIME = 60;
	
	public static final int HOW_MANY_SIMULATED_MOVES = 10;

	Random rnd;
	Node parent;
	Node[] children;
	int depth;
	int games;
	double ucb;
	double score;

	boolean player;
	
	LinkedList<Action> botActions;
	LinkedList<Action> enemyActions;

	Simulator simulator;

	LinkedList<Action> selectedBotActions;

	int botInitialHp;
	int enemyInitialHp;

	FrameData frameData;
	CommandCenter commandCenter;
	GameData gameData;

	boolean isNodeCreated;

	Deque<Action> botAction;
	Deque<Action> enemyAction;

	public Node(long ucttime, FrameData frameData, Node parent, LinkedList<Action> botActions,
			LinkedList<Action> enemyActions, GameData gameData, boolean player, CommandCenter commandCenter,
			LinkedList<Action> selectedBotActions)
	{
		this(ucttime, frameData, parent, botActions, enemyActions, gameData, player, commandCenter);
		this.selectedBotActions = selectedBotActions;
	}

	public Node(long ucttime, FrameData frameData, Node parent, LinkedList<Action> botActions,
			LinkedList<Action> enemyActions, GameData gameData, boolean player, CommandCenter commandCenter)
	{
		this.frameData = frameData;
		this.parent = parent;
		this.botActions = botActions;
		this.enemyActions = enemyActions;
		this.gameData = gameData;
		this.player = player;
		this.commandCenter = commandCenter;

		this.selectedBotActions = new LinkedList<Action>();

		this.rnd = new Random();
		this.botAction = new LinkedList<Action>();
		this.enemyAction = new LinkedList<Action>();
		
		UCT_TIME = ucttime;
		simulator = new Simulator(gameData);

		botInitialHp = frameData.getCharacter(player).getHp();
		enemyInitialHp = frameData.getCharacter(!player).getHp();

		if (parent != null)
		{
			depth = parent.depth + 1;
		}
		else
		{
			depth = 0;
		}
	}

	public Action MCTS()
	{
		long startingTime = System.nanoTime();
		for (; System.nanoTime() - startingTime <= UCT_TIME;)
		{
			UCT();
		}

		return GetBestVisitAction();
	}

	public double SimulateSomething()
	{
		botAction.clear();
		enemyAction.clear();

		for (int i = 0; i < selectedBotActions.size(); i++)
		{
			botAction.add(selectedBotActions.get(i));
		}

		for (int i = 0; i < HOW_MANY_SIMULATED_MOVES - selectedBotActions.size(); i++)
		{
			botAction.add(botActions.get(rnd.nextInt(botActions.size())));
		}

		for (int i = 0; i < HOW_MANY_SIMULATED_MOVES; i++)
		{
			enemyAction.add(enemyActions.get(rnd.nextInt(enemyActions.size())));
		}

		FrameData nFrameData = simulator.simulate(frameData, player, botAction, enemyAction, SIMULATION_TIME);

		return GetScore(nFrameData);
	}

	public double UCT()
	{
		Node selectedNode = null;
		double bestUcb = Double.MIN_VALUE;

		for (Node child : this.children)
		{
			if (child.games == 0)
			{
				child.ucb = 9999 + rnd.nextInt(50);
			}
			else
			{
				child.ucb = GetUcb(child.score / child.games, games, child.games);
			}

			if (bestUcb < child.ucb)
			{
				selectedNode = child;
				bestUcb = child.ucb;
			}

		}

		double score = 0;
		if (selectedNode.games == 0)
		{
			score = selectedNode.SimulateSomething();
		}
		else
		{
			if (selectedNode.children == null)
			{
				if (selectedNode.depth < UCT_TREE_DEPTH)
				{
					if (UCT_CREATE_NODE_THRESHOLD <= selectedNode.games)
					{
						selectedNode.CreateNode();
						selectedNode.isNodeCreated = true;
						score = selectedNode.UCT();
					}
					else
					{
						score = selectedNode.SimulateSomething();
					}
				}
				else
				{
					score = selectedNode.SimulateSomething();
				}
			}
			else
			{
				if (selectedNode.depth < UCT_TREE_DEPTH)
				{
					score = selectedNode.UCT();
				}
				else
				{
					selectedNode.SimulateSomething();
				}
			}

		}

		selectedNode.games++;
		selectedNode.score += score;

		if (depth == 0)
		{
			games++;
		}

		return score;
	}

	public void CreateNode()
	{
		children = new Node[botActions.size()];

		for (int i = 0; i < children.length; i++)
		{
			LinkedList<Action> actions = new LinkedList<Action>();
			for (Action action : selectedBotActions)
			{
				actions.add(action);
			}

			actions.add(botActions.get(i));

			children[i] = new Node(this.UCT_TIME, frameData, this, botActions, enemyActions, gameData, player,
					commandCenter, actions);
		}
	}

	public void CreateNode(LinkedList<Action> botActions)
	{
		children = new Node[botActions.size()];

		for (int i = 0; i < children.length; i++)
		{
			LinkedList<Action> actions = new LinkedList<Action>();
			for (Action action : selectedBotActions)
			{
				actions.add(action);
			}

			actions.add(botActions.get(i));

			children[i] = new Node(this.UCT_TIME, frameData, this, this.botActions, enemyActions, gameData, player, commandCenter, actions);
		}
	}

	public Action GetBestVisitAction()
	{
		int selected = -1;
		double bestGames = Double.MIN_VALUE;

		for (int i = 0; i < children.length; i++)
		{
			if (bestGames < children[i].games)
			{
				bestGames = children[i].games;
				selected = i;
			}
		}

		return children[selected].selectedBotActions.getFirst();
	}

	public int GetScore(FrameData fd)
	{
		int score = (fd.getCharacter(player).getHp() - botInitialHp) - (fd.getCharacter(!player).getHp() - enemyInitialHp);
		return score;
	}

	public double GetUcb(double score, int n, int ni)
	{
		return score + UCB_C * Math.sqrt(Math.log(n) / ni);
	}
}
