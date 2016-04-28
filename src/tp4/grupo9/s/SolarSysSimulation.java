package tp4.grupo9.s;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class SolarSysSimulation {
	
	private static final double L = Math.pow(10, 44);
	private static final double G = 6.693*Math.pow(10,-11);
	private static final double TOTAL_MASS = 2 * Math.pow(10, 30);
	private static final double MAX_DIST = Math.pow(10, 10);
	private static final double MIN_DIST = Math.pow(10, 9);
	private static final double INTER_RAD = Math.pow(10,6);
	
	private double dt, dt2;
	private Set<Particle> particles;
	
	
	public SolarSysSimulation(int N, double dt, double dt2){
		double alpha, dist;
		double mass = TOTAL_MASS / N;
		this.particles = new HashSet<Particle>();
		this.particles.add(new Particle(0, 0,0, 0, 0, 0, MIN_DIST, TOTAL_MASS, Color.YELLOW));
		for(int i=0; i<N;){
			alpha = Math.random()*2*Math.PI;
			dist = Math.random()* (MAX_DIST-MIN_DIST) + MIN_DIST;
			Particle p1 = new Particle ( dist * Math.cos(alpha), dist* Math.sin(alpha),0,0,0,0,MIN_DIST/N,mass, Color.blue);
			if(isValidPos(p1, particles)){
				particles.add(p1);
				i++;
				p1.setInitVel(L);
			}
		}
		this.dt = dt;
		this.dt2 = dt2;
	}
	
	public Vector getF(Particle p, Set<Particle> set){
		Vector force = new Vector(0,0);
		for(Particle p2: set){
			if(!p.equals(p2)){
				double alpha = Math.atan2((p2.ry-p.ry),p2.rx-p.rx);
				double f = G*p.m*p2.m/Math.pow(p.getDistance(p2),2);
				force.x += f*Math.cos(alpha);
				force.y += f*Math.sin(alpha);
			}
		}
		return force;
	}
	
	private void beeman(Set<Particle> particles){
		Set<Particle> nexts = new HashSet<>();
		for(Particle p: particles){
			p.next = new Particle(p.ID, 0, 0, 0, 0, p.r, p.m);
			p.next.rx = p.rx + p.vx*dt + (2.0/3.0)*p.f.x*dt*dt/p.m - (1.0/6.0)*p.previous.f.x*dt*dt/p.m;
			p.next.ry = p.ry + p.vy*dt + (2.0/3.0)*p.f.y*dt*dt/p.m - (1.0/6.0)*p.previous.f.y*dt*dt/p.m;
			nexts.add(p.next);
		}
		for(Particle p: particles){
			p.next.f = getF(p.next,nexts);
		}
		for(Particle p: particles){
			p.next.vx = p.vx + (1.0/3.0)*p.next.f.x*dt/p.m + (5.0/6.0)*p.f.x*dt/p.m - (1.0/6.0)*p.previous.f.x*dt/p.m; 
			p.next.vy = p.vy + (1.0/3.0)*p.next.f.y*dt/p.m + (5.0/6.0)*p.f.y*dt/p.m - (1.0/6.0)*p.previous.f.y*dt/p.m;
			
			p.previous.rx = p.rx;
			p.previous.ry = p.ry;
			p.previous.vx = p.vx;
			p.previous.vy = p.vy;
			p.previous.f = p.f;
			
			p.rx = p.next.rx;
			p.ry = p.next.ry;
			p.vx = p.next.vx;
			p.vy = p.next.vy;
		}
	}
	
	public Particle mergePar(Particle p1, Particle p2){
		double xm, ym, r, L1, L2, theta1, theta2, vxt, vyt, Vt, Z,Vf, vxf,vyf,radius;
		radius = Math.sqrt(p1.r*p1.r+p2.r*p2.r);
		xm = (p1.rx * p1.m + p2.rx *p2.m )/(p1.m + p2.m);
		ym = (p1.ry * p1.m + p2.ry *p2.m )/(p1.m + p2.m);
		r = Math.sqrt(xm*xm+ym*ym);
		// Cosine rule
		theta1 = (Math.PI/2) - Math.acos(-(Math.pow(p1.rx+p1.vx, 2)+Math.pow(p1.ry+p1.vy,2)-Math.pow(p1.distanceToOrigin(),2)-Math.pow(p1.getSpeed(),2))/(2*p1.distanceToOrigin()*p1.getSpeed()));
		theta2 = (Math.PI/2) - Math.acos(-(Math.pow(p2.rx+p2.vx, 2)+Math.pow(p2.ry+p2.vy,2)-Math.pow(p2.distanceToOrigin(),2)-Math.pow(p2.getSpeed(),2))/(2*p2.distanceToOrigin()*p2.getSpeed()));
		//theta1 = Math.acos(Math.sqrt(p1.rx*p1.rx+p1.ry*p1.ry)/Math.sqrt(p1.vx*p1.vx+p1.vy*p1.vy));
		//theta2 = Math.acos(Math.sqrt(p2.rx*p2.rx+p2.ry*p2.ry)/Math.sqrt(p2.vx*p2.vx+p2.vy*p2.vy));
		L1 = p1.m*r*Math.sqrt(p1.vx*p1.vx+p1.vy*p1.vy)*Math.sin(theta1);
		L2 = p2.m*r*Math.sqrt(p2.vx*p2.vx+p2.vy*p2.vy)*Math.sin(theta2);
		Vt = (L1+L2)/((p1.m+p2.m)*r);
		vxt = (-ym/r)*Vt;
    	vyt = (xm/r)*Vt;
    	Vf = (p1.m*p1.getSpeed()+p2.m*p2.getSpeed())/(p1.m+p2.m);
    	Z = Math.sqrt((Vf*Vf-vxt*vxt-vyt*vyt)/(vxt*vxt+vyt*vyt));
    	vxf = vxt-vyt*Z;
    	vyf = vyt+vxt*Z;
		return new Particle(xm, ym, vxf, vyf, 0, 0, radius, p1.m+p2.m, Color.black);
	}
	
	public void simulate(double totalTime){
		int runs = 0;
		double time = 0, printTime = 0;
		//checkMerge(particles);
		// Set forces and calculate previous
		Set<Particle> allPrevious = new HashSet<Particle>();
		for(Particle p: particles){
			p.f = getF(p, particles);
			Vector prevPos = eulerPos(p,-dt);
			Vector prevVel = eulerVel(p,-dt);
			p.previous = new Particle(p.ID,prevPos.x,prevPos.y,prevVel.x,prevVel.y,p.r,p.m);
			allPrevious.add(p.previous);
		}
		for(Particle p: particles){
			p.previous.f = getF(p.previous, allPrevious);
		}
		while(time<totalTime){
			if(runs%100==0)
				System.out.println(time);
			if(printTime<=time){
				Output.getInstace().write(particles, printTime);
				printTime += dt2;
			}
			beeman(particles);
			for(Particle p: particles){
				p.f = getF(p, particles);
			}
			while(checkMerge(particles));
			time += dt;
			runs++;
		}
	}
	
	private Vector eulerPos(Particle part, double dt){
		double x = part.rx + dt*part.vx + dt*dt*part.f.x/(2*part.m);
		double y = part.ry + dt*part.vy + dt*dt*part.f.y/(2*part.m);
		return new Vector(x,y);
	}
	
	private Vector eulerVel(Particle part, double dt){
		double velx = part.vx + dt*part.f.x/part.m;
		double vely = part.vy + dt*part.f.y/part.m;
		return new Vector(velx,vely);
	}
	
	 private boolean isValidPos(Particle p, Set<Particle> set){
    	for(Particle p2: set){
    		if(p.getDistance(p2)<INTER_RAD)
    			return false;
    	}
    	return true;
	 }
	 
	private boolean checkMerge(Set<Particle> particles){
		clearMarks(particles);
		for(Particle p: particles){
			p.checked = true;
			for(Particle p2: particles){
				if(!p2.checked && p.getDistance(p2)<=INTER_RAD){
					Particle merged = mergePar(p, p2);
					particles.remove(p);
					particles.remove(p2);
					particles.add(merged);
					merged.f = getF(p,particles);
					Vector prevPos = eulerPos(p,-dt);
					Vector prevVel = eulerVel(p,-dt);
					merged.previous = new Particle(merged.ID,prevPos.x,prevPos.y,prevVel.x,prevVel.y,p.r,p.m);
					merged.previous.f = getF(merged.previous, particles);
					return true;
				}
			}
		}
		return false;
	}
	
	private void clearMarks(Set<Particle> particles){
		for(Particle p: particles)
			p.checked = false;
	}

}
