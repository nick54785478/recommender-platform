import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RecommendationResponse {
  userId: string;
  items: { itemId: string; score: number; rank: number }[];
  generatedAt: string;
  fallback: boolean;
}

export interface Itinerary {
  id: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  days: number;
  imageUrl: string;
  tags: string[];
  country: string;
}

export interface BehaviorLog {
  userId: string;
  itemId: string;
  behaviorType: string;
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  
  private recommendationUrl = 'http://localhost:9002/api/v1/recommendations';
  private behaviorUrl = 'http://localhost:9001/api/v1/behaviors';

  getRecommendations(userId: string): Observable<RecommendationResponse> {
    return this.http.get<RecommendationResponse>(`${this.recommendationUrl}/${userId}`);
  }

  logBehavior(userId: string, itemId: string, behaviorType: string, customMetadata?: Record<string, string>): Observable<any> {
    const metadata = { source: 'storefront_ui', ...customMetadata };
    
    const payload = {
      userId,
      sessionId: 'session-' + Date.now(),
      itemId,
      behaviorType,
      referrerUrl: window.location.href,
      metadata
    };
    return this.http.post(`${this.behaviorUrl}/log`, payload);
  }

  getRecentBehaviors(userId: string): Observable<BehaviorLog[]> {
    return this.http.get<BehaviorLog[]>(`${this.behaviorUrl}/${userId}`);
  }

  // ==========================================
  // Mock T-Travel Backend Service
  // ==========================================
  private mockDatabase: Record<string, Itinerary> = {
    'TPE-NRT-PROMO': {
      id: 'TPE-NRT-PROMO',
      title: 'Tokyo Express - 5 Days',
      description: 'Explore the vibrant streets of Shibuya, ancient temples in Asakusa, and enjoy authentic sushi.',
      price: 15900,
      currency: 'TWD',
      days: 5,
      imageUrl: '/tokyo.png',
      tags: ['City', 'Food', 'Culture'],
      country: '🇯🇵 Japan'
    },
    'KHH-KIX-HOT': {
      id: 'KHH-KIX-HOT',
      title: 'Osaka & Kyoto Heritage',
      description: 'Experience the magic of Universal Studios Japan and the historic shrines of Kyoto.',
      price: 18500,
      currency: 'TWD',
      days: 6,
      imageUrl: '/osaka.png',
      tags: ['Theme Park', 'Heritage', 'Family'],
      country: '🇯🇵 Japan'
    },
    'TPE-BKK-SALE': {
      id: 'TPE-BKK-SALE',
      title: 'Bangkok Weekend Escape',
      description: 'Relax with Thai massages, street food tours, and vibrant night markets.',
      price: 9900,
      currency: 'TWD',
      days: 4,
      imageUrl: '/bangkok.png',
      tags: ['Relaxation', 'Nightlife', 'Budget'],
      country: '🇹🇭 Thailand'
    },
    'ICN-SEOUL-WINTER': {
      id: 'ICN-SEOUL-WINTER',
      title: 'Seoul Winter Wonder',
      description: 'Skiing, K-pop culture, and spicy street food in the heart of South Korea.',
      price: 13500,
      currency: 'TWD',
      days: 5,
      imageUrl: '/seoul.png',
      tags: ['Winter', 'Shopping', 'K-Culture'],
      country: '🇰🇷 South Korea'
    },
    'SIN-MARINA-LUX': {
      id: 'SIN-MARINA-LUX',
      title: 'Singapore Luxury Stay',
      description: 'Stay at Marina Bay Sands and explore the futuristic Gardens by the Bay.',
      price: 24000,
      currency: 'TWD',
      days: 4,
      imageUrl: '/singapore.png',
      tags: ['Luxury', 'City', 'Sightseeing'],
      country: '🇸🇬 Singapore'
    },
    'PVG-SHANGHAI-NIGHT': {
      id: 'PVG-SHANGHAI-NIGHT',
      title: 'Shanghai Neon Nights',
      description: 'Stroll along The Bund, enjoy authentic Xiaolongbao, and experience the futuristic skyline of Pudong.',
      price: 11500,
      currency: 'TWD',
      days: 4,
      imageUrl: '/shanghai.png',
      tags: ['City', 'Food', 'Nightlife'],
      country: '🇨🇳 China'
    },
    'PEK-BEIJING-HISTORY': {
      id: 'PEK-BEIJING-HISTORY',
      title: 'Beijing Imperial Heritage',
      description: 'Walk the Great Wall, explore the majestic Forbidden City, and taste the famous Peking Roast Duck.',
      price: 14200,
      currency: 'TWD',
      days: 5,
      imageUrl: '/beijing.png',
      tags: ['Heritage', 'Culture', 'Food'],
      country: '🇨🇳 China'
    },
    'CTS-HOKKAIDO-SNOW': {
      id: 'CTS-HOKKAIDO-SNOW',
      title: 'Hokkaido Snow Romance',
      description: 'Experience world-class skiing, relaxing hot springs, and delicious fresh seafood in snowy Hokkaido.',
      price: 21500,
      currency: 'TWD',
      days: 5,
      imageUrl: '/hokkaido.png',
      tags: ['Winter', 'Relaxation', 'Food'],
      country: '🇯🇵 Japan'
    },
    'OKA-OKINAWA-BEACH': {
      id: 'OKA-OKINAWA-BEACH',
      title: 'Okinawa Tropical Getaway',
      description: 'Relax on pristine white sand beaches, dive in crystal clear waters, and enjoy the unique Ryukyu culture.',
      price: 12800,
      currency: 'TWD',
      days: 4,
      imageUrl: '/okinawa.png',
      tags: ['Relaxation', 'Family', 'Culture'],
      country: '🇯🇵 Japan'
    },
    'HKG-DIMSUM-WKND': {
      id: 'HKG-DIMSUM-WKND',
      title: 'Hong Kong Dim Sum Tour',
      description: 'A quick getaway for authentic Dim Sum and Victoria Peak night views.',
      price: 8800,
      currency: 'TWD',
      days: 3,
      imageUrl: '/hongkong.png',
      tags: ['Food', 'Weekend', 'Budget'],
      country: '🇭🇰 Hong Kong'
    }
  };

  getAllItineraries(): Observable<Itinerary[]> {
    return new Observable(subscriber => {
      setTimeout(() => {
        subscriber.next(Object.values(this.mockDatabase));
        subscriber.complete();
      }, 500);
    });
  }

  getItineraryDetails(itemIds: string[]): Observable<Itinerary[]> {
    // Simulate network delay and return matched mock data
    return new Observable(subscriber => {
      setTimeout(() => {
        const details = itemIds
          .map(id => this.mockDatabase[id])
          .filter(Boolean); // Filter out any missing items
        subscriber.next(details);
        subscriber.complete();
      }, 500); // 500ms artificial delay
    });
  }
}
