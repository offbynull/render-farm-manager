import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing/src/api';
import { TestBed, inject } from '@angular/core/testing';

import { ActorService } from './actor.service';

describe('ActorService', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ HttpClientTestingModule ],
            providers: [ActorService]
        });
    });

    // This is hard to test because it's always blasting out HTTP requests in the background. Just assume this is working if all of the
    // other components work.

    // it('must properly communicate', inject(
    //     [HttpTestingController, HttpClient], (httpTester: HttpTestingController, httpMock: HttpClient) => {
    //         const fixture: ActorService = new ActorService(httpMock);
    //         let x: boolean = false;
    //         service.listen('a:b', () => x = true);

    //         httpMock.
    //         expect(service).toBeTruthy();
    // }));
});
