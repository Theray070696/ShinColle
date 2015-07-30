package com.lulan.shincolle.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.ExtendPlayerProps;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PointerItem extends BasicItem {

	IIcon[] icons = new IIcon[3];
	
	public PointerItem() {
		super();
		this.setUnlocalizedName("PointerItem");
		this.maxStackSize = 1;
		this.setHasSubtypes(true);
		this.setFull3D();
	}
	
	@Override
	public String getUnlocalizedName(ItemStack itemstack) {
		return String.format("item.%s", getUnwrappedUnlocalizedName(super.getUnlocalizedName()));
	}
	
	@Override
	public void registerIcons(IIconRegister iconRegister) {	
		icons[0] = iconRegister.registerIcon(this.getUnlocalizedName().substring(this.getUnlocalizedName().indexOf(".")+1)+"0");
		icons[1] = iconRegister.registerIcon(this.getUnlocalizedName().substring(this.getUnlocalizedName().indexOf(".")+1)+"1");
		icons[2] = iconRegister.registerIcon(this.getUnlocalizedName().substring(this.getUnlocalizedName().indexOf(".")+1)+"2");
	}
	
	@Override
	public IIcon getIconFromDamage(int meta) {
		if(meta > 2) meta = 0;
		return icons[meta];
	}
	
	//item glow effect
	@Override
	@SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack item, int pass) {
        return true;
    }
	
	/**left click (swing item)
	 * �����ĥ = �O�D�H�N�[�J����, �w�g�b����h�]��focus
	 * �ۤU���� = �������~�Ҧ� or �����w�g�b�����ship
	 */
	@Override
	public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack item) {
//		LogHelper.info("DEBUG : pointer swing (left click) "+entityLiving);
		int meta = item.getItemDamage();
		
		EntityPlayer player = null;
		if(entityLiving instanceof EntityPlayer) {
			player = (EntityPlayer) entityLiving;
		}
		
		//���a����ϥΦ��Z���� (client side only)
		if(entityLiving.worldObj.isRemote && player != null) {
			ExtendPlayerProps props = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
			MovingObjectPosition hitObj = EntityHelper.getPlayerMouseOverEntity(64D, 1F);
			
			if(hitObj != null) {
				LogHelper.info("DEBUG : pointer left click: ENTITY "+hitObj.entityHit);
				
				//�Y��ship or mounts
				if(hitObj.entityHit instanceof BasicEntityShip || hitObj.entityHit instanceof BasicEntityMount) {
					BasicEntityShip ship = null;
					//get ship entity
					if(hitObj.entityHit instanceof BasicEntityShip) {
						ship = (BasicEntityShip)hitObj.entityHit;
					}
					else {
						ship = (BasicEntityShip) ((BasicEntityMount)hitObj.entityHit).getOwner();
					}
					//null check
					if(ship == null) return false;
					
					//�O�D�H: ����: add/remove team �ۤU����:set focus
					if(EntityHelper.checkSameOwner(player, ship) && props != null) {
						//check is in team
						int i = props.checkInTeamList(ship.getEntityId());
						
						//�ۤU����: remove team if in team
						if(player.isSneaking()) {
							//if in team, remove entity
							if(i >= 0) {
								LogHelper.info("DEBUG : pointer remove team: "+ship);
								//if single mode, set other ship focus
								if(meta == 0) {
									for(int j = 0; j < 6; j++) {
										if(j != i && props.getTeamList(j) != null) {
											//focus ship j
											CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -2, 1, props.getTeamList(j).getEntityId(), meta, 0, 0));
											break;
										}
									}
								}
								
								//send add team packet (remove entity)
								CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -1, 0, ship.getEntityId()));
								return true;
							}
						}
						//����: add team or set focus if in team
						else {
							//in team, set focus
							if(i >= 0) {
								LogHelper.info("DEBUG : pointer set focus: "+hitObj.entityHit);
								CommonProxy.channelG.sendToServer(new C2SGUIPackets(player , -2, 1, ship.getEntityId(), meta, 0, 0));
							}
							//not in team, add team
							else {
								LogHelper.info("DEBUG : pointer add team: "+hitObj.entityHit);
								CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -1, 0, ship.getEntityId()));
							
								//�Ysingle mode, �h�Cadd�@���N�]�Ӱ���focus
								if(meta == 0) {
									CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -2, 1, ship.getEntityId(), meta, 0, 0));
								}
							}
							return true;
						}
					}
					//ship���D�D�H
					else {
						//�\�ॼ�w
					}
				}
				//��L��entity
				else {
					//�\�ॼ�w
				}
			}//end hit != null
			
			//�ۤU���� vs block or �D�ۤv���d��, �h����pointer�Ҧ�
			//check key pressed
			GameSettings keySet = ClientProxy.getGameSetting();
			
			if(keySet.keyBindSneak.getIsKeyPressed()) {
				//sneak+sprint: clear team
				if(keySet.keyBindSprint.getIsKeyPressed()) {
					LogHelper.info("DEBUG : pointer clear all focus");
					//send sync packet to server
					CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -7, 0, 0));
					return true;
				}
				//sneak only: change pointer mode
				else {
					meta++;
					if(meta > 2) meta = 0;
					item.setItemDamage(meta);
					
					//send sync packet to server
					CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -5, meta, 0));
					return true;
				}
			}
		}//end client side && player != null
//		//server side
//		else {
//			//server side sneaking, prevent break block
//			if(entityLiving.isSneaking()) {
//				return true;
//			}
//		}
		
        return true;	//both side
    }
	
	/**right click
	 * 
	 */
	@Override
    public ItemStack onItemRightClick(ItemStack item, World world, EntityPlayer player) {
		int meta = item.getItemDamage();
		
		//client side
		if(world.isRemote) {
			ExtendPlayerProps props = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
			
			//����getPlayerMouseOverEntity��entity
			MovingObjectPosition hitObj = EntityHelper.getPlayerMouseOverEntity(64D, 1F);
			
			//��쪺�Oentity
			if(hitObj != null && hitObj.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
				LogHelper.info("DEBUG : pointer right click: ENTITY "+hitObj.entityHit.getClass().getSimpleName());
					
				//apply guarding function
				GameSettings keySet = ClientProxy.getGameSetting();
				
				if(keySet.keyBindSprint.getIsKeyPressed()) {
					//set guard entity
					CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -6, meta, hitObj.entityHit.getEntityId()));
					return item;
				}
				
				//�Y��ship or mounts
				if(hitObj.entityHit instanceof BasicEntityShip || hitObj.entityHit instanceof BasicEntityMount) {
					BasicEntityShip ship = null;
					//get ship entity
					if(hitObj.entityHit instanceof BasicEntityShip) {
						ship = (BasicEntityShip)hitObj.entityHit;
					}
					else {
						ship = (BasicEntityShip) ((BasicEntityMount)hitObj.entityHit).getOwner();
					}
					//null check
					if(ship == null) return item;
					
					//�O�D�H: �k��: set sitting
					if(EntityHelper.checkSameOwner(player, ship)) {
						//�ۤU�k��: open GUI
						if(player.isSneaking()) {
							//send GUI packet
							CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -3, 0, ship.getEntityId()));
						}
						//�k��: set sitting
						else {
							//send sit packet
							CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -4, meta, ship.getEntityId()));
						}
						return item;
					}
					//ship���D�D�H
					else {
						//�ˬdfriendly fire, �P�w�nattack�٬O�nmove
						if(ConfigHandler.friendlyFire) {
							//attack target
							CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -2, meta, hitObj.entityHit.getEntityId()));
							//�b�ؼФW�e�X�аO
							ParticleHelper.spawnAttackParticleAtEntity(hitObj.entityHit, 0.3D, 5D, 0D, (byte)2);
						}
						else {
							//���ʨ��ship����
							CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -1, meta, 0, (int)hitObj.entityHit.posX, (int)hitObj.entityHit.posY, (int)hitObj.entityHit.posZ));
							//�b�ؼФW�e�X�аO
							ParticleHelper.spawnAttackParticleAtEntity(hitObj.entityHit, 0.3D, 4D, 0D, (byte)2);
						}
					}
				}
				//��L��entity
				else {
					if(hitObj.entityHit instanceof EntityPlayer) {
						//�ˬdfriendly fire, �P�w�nattack�٬O�nmove
						if(ConfigHandler.friendlyFire) {
							//attack target
							CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -2, meta, hitObj.entityHit.getEntityId()));
							//�b�ؼФW�e�X�аO
							ParticleHelper.spawnAttackParticleAtEntity(hitObj.entityHit, 0.3D, 5D, 0D, (byte)2);
						}
						else {
							//���ʨ��PLAYER����
							CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -1, meta, 0, (int)hitObj.entityHit.posX, (int)hitObj.entityHit.posY, (int)hitObj.entityHit.posZ));
							//�b�ؼФW�e�X�аO
							ParticleHelper.spawnAttackParticleAtEntity(hitObj.entityHit, 0.3D, 4D, 0D, (byte)2);
						}
					}
					else {
						//attack target
						CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -2, meta, hitObj.entityHit.getEntityId()));
						//�b�ؼФW�e�X�аO
						ParticleHelper.spawnAttackParticleAtEntity(hitObj.entityHit, 0.3D, 5D, 0D, (byte)2);
					}
				}
			}//end hitObj = entity
			//�Y�S���entity, �h��getPlayerMouseOverBlock��block
			else {
				MovingObjectPosition hitObj2 = EntityHelper.getPlayerMouseOverBlock(64D, 1F);
				
				if(hitObj2 != null) {
					//��쪺�Oblock
					if(hitObj2.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
						/**hit side (�A�X���ʦ�m): 0:�U��(y-1) 1:�W��(y+1) 2:�_��(z-1) 3:�n��(z+1) 4:���(x-1) 5:�F��(x+1)*/
						int x = hitObj2.blockX;
						int y = hitObj2.blockY;
						int z = hitObj2.blockZ;
						
						switch(hitObj2.sideHit) {
						default:
							y--;
							break;
						case 1:
							y++;
							break;
						case 2:
							z--;
							break;
						case 3:
							z++;
							break;
						case 4:
							x--;
							break;
						case 5:
							x++;
							break;
						}
						LogHelper.info("DEBUG : pointer right click: BLOCK: side: "+hitObj2.sideHit+" xyz: "+x+" "+y+" "+z);
						//move to xyz
						CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -1, meta, 0, x, y, z));
						//�b�ؼФW�e�X�аO
						ParticleHelper.spawnAttackParticleAt(x+0.5D, y, z+0.5D, 0.3D, 4D, 0D, (byte)25);
					}
					//���entity (�D�w�����p, ���`���Ӥ��|�A���entity)
					else if(hitObj2.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY){
						LogHelper.info("DEBUG : pointer right click: ENTITY (method 2) "+hitObj2.entityHit.getClass().getSimpleName());
						//move to entity
						//���ʨ��ship����
						CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, -1, meta, 0, (int)hitObj2.entityHit.posX, (int)hitObj2.entityHit.posY, (int)hitObj2.entityHit.posZ));
						//�b�ؼФW�e�X�аO
						ParticleHelper.spawnAttackParticleAt(hitObj2.entityHit.posX+0.5D, hitObj2.entityHit.posY, hitObj2.entityHit.posZ+0.5D, 0.3D, 4D, 0D, (byte)25);
					}
					else {
						LogHelper.info("DEBUG : pointer right click: MISS");
					}
				}//end hitObj2 = block
			}//end hitObj2 != null
		}
		
		return item;
    }
	
	//�������~��
	@Override
	public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
		LogHelper.info("DEBUG : using pointer "+count);
    }

	//right click on solid block
	@Override
	public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side, float hitx, float hity, float hitz) {
		LogHelper.info("DEBUG : use pointer "+world.isRemote+" "+player.getDisplayName()+" "+x+" "+y+" "+z+" "+side+" "+hitx+" "+hity+" "+hitz);
		return false;
    }
	
	//left click on entity
	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		return true;	//prevent this item to attack entity
    }
	
	/**�����ثe���a���۪��F��
	 * ��k1: player.rayTrace(�Z��, ticks) �ǥ�player�Y�Ҵ¦V����V��X�I�쪺�F��, �ۭq�Z��, ����G����
	 * ��k2: ClientProxy.getMineraft().renderViewEntity �C�X�Ҧ��bclient player�e�����X�{��entity, �ȧ�entity
	 * ��k3: ClientProxy.getMineraft().objectMouseOver �C�X�Ҧ��ƹ���Ы��쪺�F��, �u����Z��, ����G����
	 * ��k4: ItemStack.getMovingObjectPositionFromPlayer �C�X��Ы��쪺�F��, �u����Z��, �i��G����
	 * ��k5: �ۭqfunc_147447_a �ۦ�ק�Ѽ�, ������Z���B�i�H��G���� (�H�W��k�����ϥ�func_147447_a��k)
 	 */
	@Override
	public void onUpdate(ItemStack item, World world, Entity player, int slot, boolean inUse) {
		if(world.isRemote && !inUse) {
			//restore hotbar position
			if(item.hasTagCompound() && item.getTagCompound().getBoolean("chgHB")) {
				int orgCurrentItem = item.getTagCompound().getInteger("orgHB");
				LogHelper.info("DEBUG : change hotbar "+((EntityPlayer)player).inventory.currentItem+" to "+orgCurrentItem);
				
				if(((EntityPlayer)player).inventory.currentItem != orgCurrentItem) {
					((EntityPlayer)player).inventory.currentItem = orgCurrentItem;
					CommonProxy.channelG.sendToServer(new C2SInputPackets(1, orgCurrentItem, 0));
					item.getTagCompound().setBoolean("chgHB", false);
				}
			}
		}
		
		//show team mark
		if(inUse || ConfigHandler.alwaysShowTeam) {
			if(player instanceof EntityPlayer) {
				//client side
				if(world.isRemote) {
					if(player.ticksExisted % 10 == 0) {
						//����u�W���F�� (debug)
//						MovingObjectPosition hitObj = EntityHelper.getPlayerMouseOverEntity(64D, 1F);
//						if(hitObj != null) {
//							if(hitObj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
//								LogHelper.info("DEBUG : hit BLOCK "+world.getBlock(hitObj.blockX, hitObj.blockY, hitObj.blockZ).getLocalizedName()+" "+hitObj.blockX+" "+hitObj.blockY+" "+hitObj.blockZ);
//								float[] look = EntityHelper.getLookDegree(hitObj.blockX-player.posX, hitObj.blockY-player.posY, hitObj.blockZ-player.posZ, true);
//								LogHelper.info("DEBUG : look "+look[0]+" "+look[1]);
//							}
//							else if(hitObj.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
//								LogHelper.info("DEBUG : hit ENTITY "+hitObj.entityHit.getClass().getSimpleName());
//							}
//							else {
//								LogHelper.info("DEBUG : hit MISS ");
//							}
//						}
						
						//��ܶ�����, ��ܰ��, �i�����鵥
						ExtendPlayerProps extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
						BasicEntityShip teamship = null;
						boolean select = false;
						int meta = item.getItemDamage();
						int type = 0;
						
						if(extProps != null) {
							for(int i = 0; i < 6; i++) {
								teamship = extProps.getTeamList(i);
								
//								//debug
//								if(player.ticksExisted % 40 == 0) {
//									LogHelper.info("DEBUG : pointer: show team "+i+" "+extProps.getTeamSelected(i)+" "+teamship);
//								}
								
								if(teamship != null) {
									select = extProps.getTeamSelected(i);
									
									//�Y�O����ؼ�, �h��ܬ�pointer�C��
									if(select) {
										switch(meta) {
										default:	//default mode
											type = 1;
											break;
										case 1:		//group mode
											type = 2;
											break;
										case 2:		//formation mode
											type = 3;
											break;
										}
									}
									//�D����ؼ�, ����ܬ����, formation mode�O������
									else {
										switch(meta) {
										default:	//default mode
											type = 0;
											break;
										case 2:		//formation mode
											type = 3;
											break;
										}
									}
									
									//�b��ship�W��ܶ�����
									ParticleHelper.spawnAttackParticleAtEntity(teamship, 0.3D, type, 0D, (byte)2);
								}
							}//end team list for loop
						}
					}//end every 5 ticks
				}//end client side
			}//end is player
		}//end inUse
	}
	
	//display equip information
    @Override
    public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean par4) {  	
    	switch(itemstack.getItemDamage()) {
    	case 1:
    		list.add(EnumChatFormatting.RED + I18n.format("gui.shincolle:pointer1"));
    		list.add(EnumChatFormatting.GRAY + I18n.format("gui.shincolle:pointer3"));
    		break;
    	case 2:
    		list.add(EnumChatFormatting.GOLD + I18n.format("gui.shincolle:pointer2"));
    		list.add(EnumChatFormatting.GRAY + I18n.format("gui.shincolle:pointer3"));
    		break;
		default:
			list.add(EnumChatFormatting.AQUA + I18n.format("gui.shincolle:pointer0"));
			list.add(EnumChatFormatting.GRAY + I18n.format("gui.shincolle:pointer3"));
			break;
    	}
    	
    	ExtendPlayerProps props = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
    	
    	if(props != null) {
    		list.add(EnumChatFormatting.YELLOW+""+EnumChatFormatting.UNDERLINE + 
    				String.format("%s %d", I18n.format("gui.shincolle:pointer4"), props.getTeamId()+1));
    	
    		BasicEntityShip ship = null;
    		String name = null;
    		int level = 0;
    		int j = 1;
    		for(int i = 0; i < 6; i++) {
    			//get entity
    			ship = props.getTeamList(i);
    			
    			if(ship != null) {
    				//get level
    				level = ship.getStateMinor(ID.N.ShipLevel);
    				
	    			//get name
	    			if(ship.getCustomNameTag() != null && ship.getCustomNameTag().length() > 0) {
	    				name = ship.getCustomNameTag();
	    			}
	    			else {
	    				name = I18n.format("entity.shincolle."+ship.getClass().getSimpleName()+".name");
	    			}
	    			
	    			//add info string
	    			if(props.getTeamSelected(i)) {
	    				list.add(EnumChatFormatting.WHITE + String.format("%d: %s - Lv %d", j, name, level));
	    			}
	    			else {
	    				list.add(EnumChatFormatting.GRAY + String.format("%d: %s - Lv %d", j, name, level));
	    			}
	    			
	    			j++;
    			}
    		}
    	}
    }
	
	
    
    
	
}