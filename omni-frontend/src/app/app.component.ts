import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, RecommendationResponse } from './services/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private api = inject(ApiService);
  
  users = ['U001', 'U002', 'U003'];
  selectedUser = 'U001';
  recommendations: RecommendationResponse | null = null;
  loading = false;
  
  // Mock image mapping
  itemImages: Record<string, string> = {
    'TPE-NRT-PROMO': '/tokyo.png',
    'KHH-KIX-HOT': '/osaka.png',
    'TPE-BKK-SALE': '/bangkok.png'
  };

  ngOnInit(): void {
    this.fetchRecommendations(this.selectedUser);
  }

  selectUser(user: string): void {
    this.selectedUser = user;
    this.fetchRecommendations(user);
  }

  fetchRecommendations(userId: string): void {
    this.loading = true;
    this.api.getRecommendations(userId).subscribe({
      next: (res) => {
        this.recommendations = res;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error fetching recommendations:', err);
        this.loading = false;
      }
    });
  }

  onItemClick(itemId: string): void {
    // Log behavior to behavior-service
    this.api.logBehavior(this.selectedUser, itemId, 'CLICK').subscribe({
      next: () => console.log(`Behavior logged: CLICK on ${itemId}`),
      error: (err) => console.error('Failed to log behavior', err)
    });
  }
  
  getImage(itemId: string): string {
    return this.itemImages[itemId] || '/tokyo.png'; // Fallback
  }
}
