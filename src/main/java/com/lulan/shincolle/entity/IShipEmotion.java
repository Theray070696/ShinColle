package com.lulan.shincolle.entity;

/**SHIP EMOTION
 * include emtion time and state
 */
public interface IShipEmotion extends IShipFlags {
	
	/**Get emotion value
	 * id = 0: return damaged state
	 * id = 1: return 0:NORMAL 1:BLINK 2:T_T 3:O_O 4:BORED 5:HUNGRY
	 * id = 2: return attack emotion / tilt state
	 */
	abstract public byte getStateEmotion(int id);
	
	/**Set emotion value
	 * id: 0:damage state, 1:emotion1, 2:emotion2
	 * value: emotion type
	 * sync: send sync packet to client?
	 */
	abstract public void setStateEmotion(int id, int value, boolean sync);
	
	/**GET/SET emotion start time
	 * emotion start time for individual entity
	 */
	abstract public int getStartEmotion();
	abstract public int getStartEmotion2();
	abstract public void setStartEmotion(int par1);
	abstract public void setStartEmotion2(int par1);
	
	/**Get tick time for emotion count
	 */
	abstract public int getTickExisted();
	
	/**Get attack time, sit, run state
	 */
	abstract public int getAttackTime();
	abstract public boolean getIsRiding();
	abstract public boolean getIsSprinting();
	abstract public boolean getIsSitting();
	abstract public boolean getIsSneaking();
	abstract public boolean getIsLeashed();

}
