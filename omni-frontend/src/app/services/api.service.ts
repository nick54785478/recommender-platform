import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RecommendationResponse {
  userId: string;
  items: { itemId: string; score: number; rank: number }[];
  generatedAt: string;
  fallback: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  
  private recommendationUrl = 'http://localhost:9002/api/v1/recommendations';
  private behaviorUrl = 'http://localhost:9001/api/v1/behaviors/log';

  getRecommendations(userId: string): Observable<RecommendationResponse> {
    return this.http.get<RecommendationResponse>(`${this.recommendationUrl}/${userId}`);
  }

  logBehavior(userId: string, itemId: string, action: string): Observable<any> {
    return this.http.post(this.behaviorUrl, {
      userId,
      itemId,
      action
    });
  }
}
