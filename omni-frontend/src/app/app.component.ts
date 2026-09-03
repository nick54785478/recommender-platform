import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, RecommendationResponse, Itinerary, BehaviorLog } from './services/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private api = inject(ApiService);
  
  users = ['U001', 'U002', 'U003'];
  selectedUser = 'U001';
  
  storefrontItineraries: Itinerary[] = []; // All items
  filteredItineraries: Itinerary[] = []; // Displayed items
  
  // Pagination State
  currentPage = 1;
  pageSize = 6;

  get totalPages(): number {
    return Math.ceil(this.filteredItineraries.length / this.pageSize);
  }

  get paginatedItineraries(): Itinerary[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.filteredItineraries.slice(startIndex, startIndex + this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      // Scroll to top of storefront smoothly
      window.scrollTo({ top: 600, behavior: 'smooth' });
    }
  }

  searchTerm: string = '';
  selectedTag: string = '';
  availableTags: string[] = ['City', 'Food', 'Culture', 'Theme Park', 'Heritage', 'Family', 'Relaxation', 'Nightlife', 'Budget', 'Winter', 'Shopping', 'K-Culture', 'Luxury', 'Sightseeing', 'Weekend'];

  recommendations: RecommendationResponse | null = null;
  recommendedItineraries: Itinerary[] = []; // AI items
  recentBehaviors: BehaviorLog[] = [];
  
  loadingStorefront = false;
  loadingRecommendations = false;
  showRecommendations = false;
  showConsole = true;
  
  ngOnInit(): void {
    this.fetchStorefront();
    // 預設不要一開始就抓推薦，等使用者點擊按鈕再抓也可以，但為了保持原本邏輯，這裡還是可以先抓好
    this.fetchRecommendations(this.selectedUser);
    this.fetchRecentBehaviors();
  }

  toggleRecommendations(): void {
    this.showRecommendations = !this.showRecommendations;
    if (this.showRecommendations) {
      // 點開時重新抓取最新的推薦
      this.fetchRecommendations(this.selectedUser);
    }
  }

  selectUser(user: string): void {
    this.selectedUser = user;
    this.fetchRecommendations(user);
    this.fetchRecentBehaviors();
  }

  fetchStorefront(): void {
    this.loadingStorefront = true;
    this.api.getAllItineraries().subscribe({
      next: (items) => {
        this.storefrontItineraries = items;
        this.filteredItineraries = [...items];
        this.loadingStorefront = false;
        this.currentPage = 1;
      },
      error: (err) => {
        console.error('Error fetching storefront', err);
        this.loadingStorefront = false;
      }
    });
  }

  onSearch(): void {
    this.filteredItineraries = this.storefrontItineraries.filter(item => {
      const matchKeyword = this.searchTerm ? item.title.toLowerCase().includes(this.searchTerm.toLowerCase()) || item.description.toLowerCase().includes(this.searchTerm.toLowerCase()) : true;
      const matchTag = this.selectedTag ? item.tags.includes(this.selectedTag) : true;
      return matchKeyword && matchTag;
    });
    this.currentPage = 1; // Reset to first page on new search

    // Log the SEARCH behavior to HBase with keyword metadata
    if (this.searchTerm || this.selectedTag) {
      const keyword = `${this.searchTerm} ${this.selectedTag}`.trim();
      this.api.logBehavior(this.selectedUser, '', 'SEARCH', { keyword }).subscribe({
        next: () => {
          console.log(`[Behavior Tracked] User ${this.selectedUser} searched for ${keyword}`);
          setTimeout(() => this.fetchRecentBehaviors(), 300);
        }
      });
    }
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.selectedTag = '';
    this.onSearch();
  }

  fetchRecommendations(userId: string): void {
    this.loadingRecommendations = true;
    this.api.getRecommendations(userId).subscribe({
      next: (res) => {
        this.recommendations = res;
        const itemIds = res.items.map(item => item.itemId);
        this.api.getItineraryDetails(itemIds).subscribe({
          next: (details) => {
            // Sort details to match the score ranking
            const sortedDetails = res.items
              .map(rec => details.find(d => d.id === rec.itemId))
              .filter(Boolean) as Itinerary[];
            this.recommendedItineraries = sortedDetails;
            this.loadingRecommendations = false;
          },
          error: (err) => {
            console.error('Error fetching itinerary details:', err);
            this.loadingRecommendations = false;
          }
        });
      },
      error: (err) => {
        console.error('Error fetching recommendations:', err);
        this.loadingRecommendations = false;
      }
    });
  }

  fetchRecentBehaviors(): void {
    this.api.getRecentBehaviors(this.selectedUser).subscribe({
      next: (logs) => {
        this.recentBehaviors = logs;
      },
      error: (err) => {
        console.error('Error fetching recent behaviors:', err);
      }
    });
  }

  selectedItinerary: Itinerary | null = null;

  handleAction(itemId: string, action: string, message: string): void {
    this.api.logBehavior(this.selectedUser, itemId, action).subscribe({
      next: () => {
        console.log(`[Behavior Tracked] User ${this.selectedUser} performed ${action} on ${itemId}`);
        setTimeout(() => this.fetchRecentBehaviors(), 300);
      },
      error: (err) => console.error('Failed to log behavior', err)
    });
  }

  onView(item: Itinerary): void {
    this.selectedItinerary = item;
    this.handleAction(item.id, 'VIEW', `Viewing details for ${item.id}`);
  }

  closeQuickView(): void {
    this.selectedItinerary = null;
  }

  onLike(itemId: string): void {
    this.handleAction(itemId, 'LIKE', `Added ${itemId} to your wish list ❤️`);
  }

  onAddToCart(itemId: string): void {
    this.handleAction(itemId, 'ADD_TO_CART', `Added ${itemId} to your cart 🛒`);
  }

  onPurchase(itemId: string): void {
    this.handleAction(itemId, 'PURCHASE', `Thank you for booking ${itemId}! 💳`);
  }
}
