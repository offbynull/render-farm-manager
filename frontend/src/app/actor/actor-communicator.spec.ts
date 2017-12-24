import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestBed, fakeAsync, inject, tick } from '@angular/core/testing';

import { ActorCommunicator } from './actor-communicator';
import { Message } from './message';
import { Address } from './address';

describe('ActorCommunicator', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ HttpClientTestingModule ],
            providers: []
        });
    });

    it('must report on good /address response',
        inject([HttpTestingController, HttpClient], (httpTester: HttpTestingController, httpMock: HttpClient) => {
            let actual: Address | undefined;
            const cb = (err: Error | undefined, address: Address | undefined) => {
                actual = address;
            };



            const fixture: ActorCommunicator = new ActorCommunicator('http://test', httpMock);
            fixture.address(cb);

            const httpReq: TestRequest = httpTester.expectOne('http://test/address');
            const httpRespData: string = 'servlet:addr1';
            httpReq.flush(httpRespData);



            httpTester.verify();
            expect(actual).toBeDefined();

            actual = <Address> actual; // narrow type to just Message[] -- cannot be undefined

            expect(actual.toString()).toEqual('servlet:addr1');
        })
    );

    it('must report on /address communication errors',
        inject([HttpTestingController, HttpClient], (httpTester: HttpTestingController, httpMock: HttpClient) => {
            let actualError: any;
            const cb = (err: any, outQueue: Address | undefined) => {
                actualError = err;
            };



            const fixture: ActorCommunicator = new ActorCommunicator('http://test', httpMock);
            fixture.address(cb);

            const httpReq: TestRequest = httpTester.expectOne('http://test/address');
            httpReq.error(new ErrorEvent('error'), { headers: undefined, status: 500, statusText: 'Server Error'});



            httpTester.verify();
            expect(actualError).toBeDefined();
        })
    );

    it('must report on good /rfm response',
        inject([HttpTestingController, HttpClient], (httpTester: HttpTestingController, httpMock: HttpClient) => {
            let actual: Message[] | undefined;
            const cb = (err: Error | undefined, outQueue: Message[] | undefined) => {
                actual = outQueue;
            };



            const fixture: ActorCommunicator = new ActorCommunicator('http://test', httpMock);
            fixture.communicate('aaaa', 0, 0, [] , cb);

            const httpReq: TestRequest = httpTester.expectOne('http://test/rfm');
            const httpRespData: Object = {
                outQueue: [
                    {
                        source: 'actor:worker789:querier',
                        destination: 'servlet:0782d5a941fc97cabf18:subsystem1',
                        type: 'java.lang.String',
                        data: 'WORK_DONE'
                    },
                    {
                        source: 'actor:worker1',
                        destination: 'servlet:0782d5a941fc97cabf18:subsystem2',
                        type: 'com.test',
                        data: { field1: 'aa', field2: 3 }
                    }
                ]
            };
            httpReq.flush(httpRespData);



            httpTester.verify();
            expect(actual).toBeDefined();

            actual = <Message[]> actual; // narrow type to just Message[] -- cannot be undefined

            expect(actual.length).toEqual(2);
            expect(actual[0].source.toString()).toEqual('actor:worker789:querier');
            expect(actual[0].destination.toString()).toEqual('servlet:0782d5a941fc97cabf18:subsystem1');
            expect(actual[0].type).toEqual('java.lang.String');
            expect(actual[0].data).toEqual('WORK_DONE');
            expect(actual[1].source.toString()).toEqual('actor:worker1');
            expect(actual[1].destination.toString()).toEqual('servlet:0782d5a941fc97cabf18:subsystem2');
            expect(actual[1].type).toEqual('com.test');
            expect(actual[1].data).toEqual({ field1: 'aa', field2: 3 });
        })
    );

    it('must report on malformed /rfm response',
        inject([HttpTestingController, HttpClient], (httpTester: HttpTestingController, httpMock: HttpClient) => {
            let actualError: any;
            const cb = (err: any, outQueue: Message[] | undefined) => {
                actualError = err;
            };



            const fixture: ActorCommunicator = new ActorCommunicator('http://test', httpMock);
            fixture.communicate('aaaa', 0, 0, [] , cb);

            const httpReq: TestRequest = httpTester.expectOne('http://test/rfm');
            const httpRespData: Object = {
                outQueue: [
                    {
                        source: 'actor:worker789:querier',
                        destination: 'servlet:0782d5a941fc97cabf18:subsystem1',
                        type: 'java.lang.String',
                        data: 'WORK_DONE'
                    },
                    {
                        // SOURCE MISSING IN SECOND MESSAGE
                        destination: 'servlet:0782d5a941fc97cabf18:subsystem2',
                        type: 'com.test',
                        data: { field1: 'aa', field2: 3 }
                    }
                ]
            };
            httpReq.flush(httpRespData);


            httpTester.verify();
            expect(actualError).toBeDefined();
        })
    );

    it('must report on /rfm communication errors',
        inject([HttpTestingController, HttpClient], (httpTester: HttpTestingController, httpMock: HttpClient) => {
            let actualError: any;
            const cb = (err: any, outQueue: Message[] | undefined) => {
                actualError = err;
            };



            const fixture: ActorCommunicator = new ActorCommunicator('http://test', httpMock);
            fixture.communicate('aaaa', 0, 0, [] , cb);

            const httpReq: TestRequest = httpTester.expectOne('http://test/rfm');
            httpReq.error(new ErrorEvent('error'), { headers: undefined, status: 500, statusText: 'Server Error'});



            httpTester.verify();
            expect(actualError).toBeDefined();
        })
    );
});
