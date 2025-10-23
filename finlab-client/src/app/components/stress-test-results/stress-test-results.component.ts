import { Component, OnInit } from '@angular/core';
import { StressTestResultService, TestResultListItem, TestResultSummary } from '../../services/stress-test-result.service';

@Component({
  selector: 'app-stress-test-results',
  templateUrl: './stress-test-results.component.html',
  styleUrls: ['./stress-test-results.component.css']
})
export class StressTestResultsComponent implements OnInit {
  testResults: TestResultListItem[] = [];
  selectedTest: TestResultSummary | null = null;
  loading: boolean = false;
  error: string | null = null;

  constructor(private service: StressTestResultService) {}

  ngOnInit() {
    this.loadTestResults();
  }

  loadTestResults() {
    this.loading = true;
    this.error = null;

    this.service.listTestResults().subscribe({
      next: results => {
        this.testResults = results;
        this.loading = false;
      },
      error: err => {
        this.error = 'Failed to load test results';
        this.loading = false;
      }
    });
  }

  viewDetails(testId: string) {
    this.loading = true;
    this.error = null;

    this.service.getTestResultSummary(testId).subscribe({
      next: summary => {
        this.selectedTest = summary;
        this.loading = false;
      },
      error: err => {
        this.error = 'Failed to load test details';
        this.loading = false;
      }
    });
  }

  closeDetails() {
    this.selectedTest = null;
  }

  getPerformanceClass(testName: string, avgResponseTime: number): string {
    const isNormalLoad = testName.toLowerCase().includes('normal');
    const threshold = isNormalLoad ? 500 : 1000;

    if (avgResponseTime < threshold * 0.6) return 'status-success';
    if (avgResponseTime < threshold) return 'status-warning';
    return 'status-danger';
  }
}
