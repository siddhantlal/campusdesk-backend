package dev.campusdesk.categories;

import jakarta.persistence.*;

@Entity @Table(name="categories")
public class Category {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=80) private String name;
    @Column(nullable=false,length=500) private String description;
    @Column(nullable=false) private boolean active;
    @Version private long version;
    protected Category() {}
    public Category(String name,String description) { this.name=name.trim(); this.description=description.trim(); this.active=true; }
    public void update(String name,String description,boolean active) { this.name=name.trim(); this.description=description.trim(); this.active=active; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
