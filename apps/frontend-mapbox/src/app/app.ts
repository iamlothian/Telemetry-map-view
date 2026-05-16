import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FeedStatusPanelComponent } from './features/map/feed-status-panel.component';

@Component({
  imports: [RouterModule, FeedStatusPanelComponent],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected title = 'Telemetry Map View';
}
