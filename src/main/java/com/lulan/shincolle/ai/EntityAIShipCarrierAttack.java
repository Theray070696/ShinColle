package com.lulan.shincolle.ai;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.entity.IShipAircraftAttack;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

/**CARRIER RANGE ATTACK AI
 * entity必須實作IUseAircraft
 */
public class EntityAIShipCarrierAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private IShipAircraftAttack host;  	//entity with AI
    private EntityLiving host2;
    private EntityLivingBase target;  	//entity of target
    private int launchDelay;			//aircraft launch delay
    private int launchDelayMax;			//max launch delay
    private boolean launchType;			//airplane type, true = light
    private float range;			//attack range
    private float rangeSq;				//attack range square
    
    //直線前進用餐數
    private double distSq, distX, distY, distZ;	//跟目標的直線距離(的平方)
    
 
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipCarrierAttack(IShipAircraftAttack host) {
        if (!(host instanceof IShipAircraftAttack)) {
            throw new IllegalArgumentException("CarrierAttack AI requires IShipAircraftAttack");
        }
        else {
            this.host = host;
            this.host2 = (EntityLiving) host;
            this.setMutexBits(4);
            
            //init value
            this.launchDelay = 20;
            this.launchDelayMax = 40;
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
//    	LogHelper.info("DEBUG : carrier attack "+target);
    	if(this.host.getIsSitting()) return false;
    	
    	EntityLivingBase target = this.host.getTarget();

        if((target != null && target.isEntityAlive()) &&
           ((this.host.getStateFlag(ID.F.UseAirLight) && this.host.hasAmmoLight() && this.host.hasAirLight()) || 
            (this.host.getStateFlag(ID.F.UseAirHeavy) && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()))) {   
        	this.target = target;
        	return true;
        }
        
        return false;
    }
    
    //init AI parameter, call once every target, DO NOT reset delay time here
    @Override
    public void startExecuting() {
        distSq = distX = distY = distZ = 0D;
    }

    //判定是否繼續AI： 有target就繼續, 或者還在移動中則繼續
    public boolean continueExecuting() {
//        return this.shouldExecute() || !this.host.getShipNavigate().noPath();
    	return this.shouldExecute();
    }

    //重置AI方法, DO NOT reset delay time here
    public void resetTask() {
//    	LogHelper.info("DEBUG : air attack AI "+target);
        this.target = null;
        if(host != null) {
        	this.host2.setAttackTarget(null);
        	this.host.getShipNavigate().clearPathEntity();
        }
    }

    //進行AI
    public void updateTask() {
    	if(this.target != null && host != null) {  //for lots of NPE issue-.-
    		boolean onSight = this.host2.getEntitySenses().canSee(this.target);
//    		boolean onSight = true;		 //for debug
    		
    		//get update attributes
        	if(this.host2.ticksExisted % 60 == 0) {	
        		this.launchDelayMax = (int)(60F / (this.host.getAttackSpeed())) + 10;
                this.range = this.host.getAttackRange();
                this.rangeSq = this.range * this.range;
        	}

    		if(this.distSq >= this.rangeSq) {
    			this.distX = this.target.posX - this.host2.posX;
        		this.distY = this.target.posY - this.host2.posY;
        		this.distZ = this.target.posZ - this.host2.posZ;	
        		this.distSq = distX*distX + distY*distY + distZ*distZ;
    	
        		//若目標進入射程, 且目標無障礙物阻擋, 則清空AI移動的目標, 以停止繼續移動      
		        if(distSq < (double)this.rangeSq && onSight) {
		        	this.host.getShipNavigate().clearPathEntity();
		        }
		        else {	//目標移動, 則繼續追擊		        	
		        	if(host2.ticksExisted % 32 == 0) {
		        		this.host.getShipNavigate().tryMoveToEntityLiving(this.target, 1D);
		        	}
	            }
    		}//end dist > range
	
	        //設定攻擊時, 頭部觀看的角度
    		this.host2.getLookHelper().setLookPosition(this.target.posX, this.target.posY+2D, this.target.posZ, 30.0F, 60.0F);
	         
	        //delay time decr
	        this.launchDelay--;

	        //若只使用單一種彈藥, 則停用型態切換, 只發射同一種飛機
	        if(!this.host.getStateFlag(ID.F.UseAirLight)) {
	        	this.launchType = false;
	        }
	        if(!this.host.getStateFlag(ID.F.UseAirHeavy)) {
	        	this.launchType = true;
	        }
	        
	        //若attack delay倒數完了, 則開始攻擊
	        if(onSight && this.distSq <= this.rangeSq && this.launchDelay <= 0) {
	        	//使用輕攻擊
	        	if(this.launchType && this.host.hasAmmoLight() && this.host.hasAirLight()) {
	        		this.host.attackEntityWithAircraft(this.target);
		            this.launchDelay = this.launchDelayMax;	//reset delay
	        	}
	        	
	        	//使用重攻擊
	        	if(!this.launchType && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()) {
	        		this.host.attackEntityWithHeavyAircraft(this.target);
	        		this.launchDelay = this.launchDelayMax;	//reset delay
	        	}
	        	
	        	this.launchType = !this.launchType;		//change type
	        }
	        
	        //若超過太久都打不到目標(或是追不到), 則重置目標
	        if(this.launchDelay < -40) {
	        	this.launchDelay = 20;
	        	this.resetTask();
	        	return;
	        }
    	}
    }
}
