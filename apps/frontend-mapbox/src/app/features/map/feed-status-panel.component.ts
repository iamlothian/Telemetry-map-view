import { Component, inject } from '@angular/core';
import { FeedStatus } from 'shared-models';
import { TelemetryStreamService } from '../../core/telemetry/telemetry-stream.service';

@Component({
  selector: 'app-feed-status-panel',
  template: `
    <div class="feed-panel">
      <h3>Feed Status</h3>
      @for (status of feedStatusList(); track status.feedId) {
        <div class="feed-item" [class.healthy]="status.healthy" [class.unhealthy]="!status.healthy">
          <span class="indicator" [attr.aria-label]="status.healthy ? 'healthy' : 'degraded'"></span>
          <span class="name">{{ status.name }}</span>
          @if (status.message) {
            <span class="message">{{ status.message }}</span>
          }
        </div>
      }
      @if (feedStatusList().length === 0) {
        <p class="empty">No feeds reported yet.</p>
      }
    </div>
  `,
  styleUrl: './feed-status-panel.component.scss',
})
export class FeedStatusPanelComponent {
  private readonly stream = inject(TelemetryStreamService);

  feedStatusList(): FeedStatus[] {
    return Array.from(this.stream.feedStatuses().values());
  }
}
