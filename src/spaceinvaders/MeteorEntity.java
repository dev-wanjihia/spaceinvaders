/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spaceinvaders;

/**
 *
 * @author Samson
 */
public class MeteorEntity extends Entity {

    Game game;

    public MeteorEntity(Game game, int x, int y) {
        super("assets/drawables/meteor.gif", x, y);
        this.game = game;
        this.dy = 100;
    }
    
    
    @Override
    public void collidedWith(Entity other) {
        if(other instanceof ShipEntity){
            game.notifyDeath();
        }else if(other instanceof ShotEntity){
            game.createExplosionAt(x, y);
            game.removeEntity(this);
            game.removeEntity(other);
        }else if(other instanceof MeteorEntity){
            int avgVelocity = (int)(other.dy + this.dy )/2;
            this.dy = avgVelocity;
            other.dy = avgVelocity;
        }
    }
    
    @Override
    public void move(long delta){
        super.move(delta);
        
        if(y > 600)
            game.removeEntity(this);
    }
}
