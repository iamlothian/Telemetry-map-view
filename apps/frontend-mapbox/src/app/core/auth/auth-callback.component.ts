import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Handles the OIDC redirect callback at /auth/callback */
@Component({
  selector: 'app-auth-callback',
  template: `<p>Authenticating…</p>`,
})
export class AuthCallbackComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (code) {
      this.auth.handleCallback(code);
      this.router.navigate(['/']);
    } else {
      this.router.navigate(['/']);
    }
  }
}
